package com.meraki.letgohome.route

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

internal fun Context.hasPreciseLocation() =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

internal fun Context.hasForegroundLocation() =
    hasPreciseLocation() ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

@SuppressLint("MissingPermission")
internal suspend fun currentPosition(context: Context): GeoPoint? {
    if (!context.hasForegroundLocation()) return null
    val manager = context.getSystemService(LocationManager::class.java) ?: return null
    val providers = if (context.hasPreciseLocation()) {
        listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
    } else {
        listOf(LocationManager.NETWORK_PROVIDER)
    }

    for (provider in providers) {
        if (!manager.isProviderEnabled(provider)) continue
        val point = withTimeoutOrNull(7_000) {
            suspendCancellableCoroutine { continuation ->
                val signal = CancellationSignal()
                continuation.invokeOnCancellation { signal.cancel() }
                try {
                    LocationManagerCompat.getCurrentLocation(
                        manager, provider, signal, ContextCompat.getMainExecutor(context),
                    ) { location ->
                        continuation.resume(location?.let { GeoPoint(it.latitude, it.longitude) })
                    }
                } catch (_: SecurityException) {
                    continuation.resume(null)
                } catch (_: IllegalArgumentException) {
                    continuation.resume(null)
                }
            }
        }
        if (point != null) return point
    }
    return null
}
