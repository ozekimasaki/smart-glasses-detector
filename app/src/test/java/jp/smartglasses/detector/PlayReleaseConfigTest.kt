package jp.smartglasses.detector

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PlayReleaseConfigTest {
    @Test
    fun `play submission uses android 16 target and connected-device scanning`() {
        val gradle = locate("app/build.gradle.kts").readText()
        assertTrue(gradle.contains("compileSdk = 37"))
        assertTrue(gradle.contains("targetSdk = 36"))
        assertTrue(gradle.contains("minSdk = 26"))
        assertTrue(gradle.contains("isMinifyEnabled = true"))
        assertTrue(gradle.contains("isShrinkResources = true"))
        assertTrue(gradle.contains("useLegacyPackaging = false"))

        val manifest = locate("app/src/main/AndroidManifest.xml").readText()
        assertTrue(manifest.contains("android:usesPermissionFlags=\"neverForLocation\""))
        assertTrue(manifest.contains("android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE"))
        assertTrue(manifest.contains("android:foregroundServiceType=\"connectedDevice\""))
        assertTrue(manifest.contains("android:enableOnBackInvokedCallback=\"true\""))
        assertTrue(manifest.contains("android:intentMatchingFlags=\"enforceIntentFilter\""))
        assertFalse(manifest.contains("LOCKED_BOOT_COMPLETED"))
        assertFalse(manifest.contains("REQUEST_IGNORE_BATTERY_OPTIMIZATIONS"))
    }

    private fun locate(relativePath: String): File {
        val userDir = System.getProperty("user.dir")
            ?: error("user.dir is missing")
        var dir: File? = File(userDir).canonicalFile
        repeat(6) {
            val current = dir ?: return@repeat
            val candidate = File(current, relativePath)
            if (candidate.isFile) {
                return candidate
            }
            dir = current.parentFile
        }
        error("$relativePath was not found from ${System.getProperty("user.dir")}")
    }
}
