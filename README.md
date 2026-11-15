# 렛고홈

전기차로 차박할 때 귀가에 필요한 전력을 먼저 남겨두고, 차박에 사용할 수 있는 전력을 확인하는 Android 앱 시안입니다.

- 귀가 화면에서 목적지를 검색하거나 지도에서 선택합니다. 경로 거리와 전비, 안전 여유를 바탕으로 귀가용 전력을 계산합니다.
- 차박을 시작하면 현재 배터리와 귀가용·차박용 전력을 확인할 수 있습니다.
- 차량 전력 화면의 배터리 잔량과 충전 상태는 시연 데이터입니다.

## 빌드

JDK 17, Android SDK Platform 35와 Build Tools 35.0.0이 필요합니다. Android Studio에서 프로젝트를 열거나 `ANDROID_HOME`에 SDK 경로를 지정합니다. 앱은 Android API 28 이상에서 실행됩니다.

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

## 에뮬레이터 시연

키 없이 시연하려면 에뮬레이터를 시작하고 다음과 같이 로컬 중계를 연결합니다.

```sh
# 터미널 1
python3 tools/kakao_route_proxy.py --demo

# 터미널 2
adb reverse tcp:8765 tcp:8765
./gradlew :app:installDebug
```

렛고홈에서 **귀가 → 한강공원 검색 → 결과 선택 → 홈 → 차박 시작** 순서로 진행합니다. 차박 시작 화면을 아래로 스크롤하면 전력 배분을 확인할 수 있습니다.

실제 카카오 지도와 길찾기를 사용하려면 카카오 앱에 Android 패키지 `com.meraki.letgohome`과 디버그 서명 키 해시를 등록합니다. 로컬 `local.properties`의 `KAKAO_MAP_NATIVE_APP_KEY`에 네이티브 앱 키를, `.env.kakao`의 `KAKAO_MOBILITY_REST_API_KEY`에 REST 키를 설정합니다. 장소 검색에 별도 키를 쓸 때는 `KAKAO_LOCAL_REST_API_KEY`를 지정합니다. 중계에서 `--demo`를 빼고 앱을 다시 빌드한 뒤 실행합니다.

- [카카오 지도 Android SDK](https://apis.map.kakao.com/android_v2/docs/getting-started/quickstart/)
- [카카오 로컬 검색 API](https://developers.kakao.com/docs/ko/local/dev-guide)
- [카카오모빌리티 자동차 길찾기](https://developers.kakaomobility.com/guide/navi-api/directions)
