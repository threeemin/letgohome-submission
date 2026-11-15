"""Local Android demo proxy. The Kakao REST key stays in the host environment."""

import argparse
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
import os
from pathlib import Path
from urllib.error import URLError
from urllib.parse import parse_qs, urlencode, urlsplit
from urllib.request import HTTPRedirectHandler, Request, build_opener


class RejectRedirects(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        # Do not forward the API key to a redirect destination.
        return None


def coordinate(query, name):
    raw, = query[name]
    longitude, latitude = map(float, raw.split(","))
    if not (-180 <= longitude <= 180 and -90 <= latitude <= 90):
        raise ValueError("Invalid coordinate")
    return longitude, latitude


def demo_response(origin, destination):
    x, y = origin
    end_x, end_y = destination
    return {
        "routes": [{
            "result_code": 0,
            "summary": {"distance": 32500, "duration": 2520},
            "sections": [{"roads": [{
                "vertexes": [x, y, x, (2 * y + end_y) / 3,
                             (x + end_x) / 2, (y + end_y) / 2, end_x, end_y]
            }]}],
        }]
    }


class RouteHandler(BaseHTTPRequestHandler):
    def log_message(self, format, *args):
        # Avoid logging the URL because it contains the user's coordinates.
        pass

    def reply(self, status, data):
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Cache-Control", "no-store")
        self.send_header("Content-Length", str(len(data)))
        if self.server.demo:
            self.send_header("X-LetGoHome-Demo", "1")
        self.end_headers()
        self.wfile.write(data)

    def do_GET(self):
        request = urlsplit(self.path)
        if request.path == "/search":
            self.search(request.query)
            return
        if request.path != "/route":
            self.reply(404, b'{"error":"not_found"}')
            return
        try:
            query = parse_qs(request.query, max_num_fields=2)
            origin = coordinate(query, "origin")
            destination = coordinate(query, "destination")
        except (KeyError, ValueError):
            self.reply(400, b'{"error":"invalid_coordinates"}')
            return
        if self.server.demo:
            self.reply(200, json.dumps(demo_response(origin, destination)).encode())
            return
        params = urlencode({
            "origin": f"{origin[0]},{origin[1]}",
            "destination": f"{destination[0]},{destination[1]}",
            "summary": "false",
            "priority": "RECOMMEND",
            "alternatives": "false",
        })
        upstream = Request(
            f"https://apis-navi.kakaomobility.com/v1/directions?{params}",
            headers={"Authorization": f"KakaoAK {self.server.rest_key}"},
        )
        try:
            with build_opener(RejectRedirects()).open(upstream, timeout=10) as response:
                data = response.read(4_000_001)
            if len(data) > 4_000_000:
                self.reply(502, b'{"error":"route_too_large"}')
                return
        except (URLError, OSError):
            self.reply(502, b'{"error":"kakao_unavailable"}')
            return
        self.reply(200, data)


    def search(self, query_string):
        try:
            query, = parse_qs(query_string, max_num_fields=1)["q"]
            term = query.strip()
            if not 1 <= len(term) <= 100:
                raise ValueError("Invalid search")
        except (KeyError, ValueError):
            self.reply(400, b'{"error":"invalid_query"}')
            return

        if self.server.demo:
            samples = (
                {"name": "서울시청", "address": "서울 중구 세종대로 110", "lat": 37.5664, "lon": 126.9769},
                {"name": "한강공원", "address": "서울 영등포구 여의동로 330", "lat": 37.5284, "lon": 126.9331},
            )
            aliases = ("Seoul City Hall", "Hangang Park")
            normalized = term.casefold()
            places = [
                place for place, alias in zip(samples, aliases)
                if term in place["name"] or term in place["address"] or normalized in alias.casefold()
            ]
        else:
            try:
                places = self.search_kakao(term, "keyword")
                if not places:
                    places = self.search_kakao(term, "address")
            except (URLError, OSError, ValueError, KeyError, TypeError):
                self.reply(502, b'{"error":"kakao_search_unavailable"}')
                return
        self.reply(200, json.dumps({"places": places}, ensure_ascii=False).encode())

    def search_kakao(self, term, kind):
        params = urlencode({"query": term, "size": 5})
        upstream = Request(
            f"https://dapi.kakao.com/v2/local/search/{kind}.json?{params}",
            headers={"Authorization": f"KakaoAK {self.server.search_key}"},
        )
        with build_opener(RejectRedirects()).open(upstream, timeout=5) as response:
            data = response.read(128_001)
        if len(data) > 128_000:
            raise ValueError("Search response too large")
        places = []
        for item in json.loads(data)["documents"]:
            address = item.get("road_address_name") or item.get("address_name", "")
            places.append({
                "name": item.get("place_name") or address,
                "address": address,
                "lat": float(item["y"]),
                "lon": float(item["x"]),
            })
        return places


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--demo", action="store_true", help="Return a labelled synthetic route")
    parser.add_argument("--port", type=int, default=8765)
    args = parser.parse_args()
    local_file = Path(__file__).resolve().parent.parent / ".env.kakao"
    settings = {}
    if not args.demo and local_file.is_file():
        for line in local_file.read_text(encoding="utf-8").splitlines():
            name, separator, value = line.partition("=")
            if separator:
                settings[name.strip()] = value.strip().strip("\"'")
    key = os.environ.get("KAKAO_MOBILITY_REST_API_KEY", "").strip() or settings.get("KAKAO_MOBILITY_REST_API_KEY")
    search_key = os.environ.get("KAKAO_LOCAL_REST_API_KEY", "").strip() or settings.get("KAKAO_LOCAL_REST_API_KEY") or key
    if not args.demo and not key:
        parser.error("Set KAKAO_MOBILITY_REST_API_KEY or use --demo")
    with ThreadingHTTPServer(("127.0.0.1", args.port), RouteHandler) as server:
        server.demo = args.demo
        server.rest_key = key
        server.search_key = search_key
        print(f"Route proxy listening on 127.0.0.1:{server.server_port} ({'demo' if args.demo else 'Kakao'})", flush=True)
        server.serve_forever()


if __name__ == "__main__":
    main()
