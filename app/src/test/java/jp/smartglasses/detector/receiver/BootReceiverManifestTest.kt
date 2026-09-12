package jp.smartglasses.detector.receiver

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BootReceiverManifestTest {
    @Test
    fun `boot receiver restores scanning after boot bluetooth and location changes`() {
        val manifest = locateManifest().readText()

        assertTrue(manifest.contains("android.permission.RECEIVE_BOOT_COMPLETED"))
        assertTrue(manifest.contains(".receiver.BootReceiver"))
        assertTrue(manifest.contains("android.intent.action.BOOT_COMPLETED"))
        assertTrue(manifest.contains("android.intent.action.MY_PACKAGE_REPLACED"))
        assertTrue(manifest.contains("android.intent.action.USER_UNLOCKED"))
        assertTrue(manifest.contains("android.bluetooth.adapter.action.STATE_CHANGED"))
        assertTrue(manifest.contains("android.location.MODE_CHANGED"))
    }

    private fun locateManifest(): File {
        val userDir = System.getProperty("user.dir")
            ?: error("user.dir is missing")
        var dir: File? = File(userDir).canonicalFile
        repeat(6) {
            val current = dir ?: return@repeat
            listOf(
                File(current, "app/src/main/AndroidManifest.xml"),
                File(current, "src/main/AndroidManifest.xml")
            ).forEach { candidate ->
                if (candidate.isFile) {
                    return candidate
                }
            }
            dir = current.parentFile
        }
        error("app/src/main/AndroidManifest.xml was not found from ${System.getProperty("user.dir")}")
    }
}
