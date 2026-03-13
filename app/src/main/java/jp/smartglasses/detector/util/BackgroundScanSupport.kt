package jp.smartglasses.detector.util

import android.os.Build

object BackgroundScanSupport {
    fun isSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    fun isEnabled(backgroundEnabled: Boolean): Boolean {
        return isSupported() && backgroundEnabled
    }
}
