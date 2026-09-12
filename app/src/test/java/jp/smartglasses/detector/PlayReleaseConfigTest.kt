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

        val privacy = locate("app/src/main/res/values/strings.xml").readText()
        assertTrue(
            privacy.contains(
                ">https://smart-glasses-detector-policy.maigo999.workers.dev<"
            )
        )
    }

    @Test
    fun `gradle dependencies stay on the latest stable releases`() {
        val catalog = locate("gradle/libs.versions.toml").readText()
        val wrapper = locate("gradle/wrapper/gradle-wrapper.properties").readText()

        mapOf(
            "agp" to "9.4.0",
            "kotlin" to "2.4.20",
            "ksp" to "2.3.12",
            "composeBom" to "2026.09.00",
            "hilt" to "2.60.1",
            "room" to "2.8.5",
            "navigationCompose" to "2.10.1",
            "datastore" to "1.2.1",
            "lifecycleRuntimeKtx" to "2.11.0",
            "activityCompose" to "1.13.0",
            "coreKtx" to "1.19.0"
        ).forEach { (key, version) ->
            assertTrue(
                "$key should be pinned to stable $version",
                catalog.contains("""$key = "$version"""")
            )
        }

        assertTrue(wrapper.contains("gradle-9.7.1-bin.zip"))
        assertFalse(catalog.contains("9.5.0-alpha"))
        assertFalse(catalog.contains("compose-bom-alpha"))
        assertFalse(catalog.contains("1.3.0-alpha"))
    }

    @Test
    fun `github release workflow publishes signed apk on version tags`() {
        val workflow = locate(".github/workflows/release.yml").readText()
        assertTrue(workflow.contains("tags:"))
        assertTrue(workflow.contains("\"v*\""))
        assertTrue(workflow.contains("assembleRelease"))
        assertTrue(workflow.contains("bundleRelease"))
        assertTrue(workflow.contains("softprops/action-gh-release@v3"))
        assertTrue(workflow.contains("app-release.apk"))
        assertTrue(workflow.contains("contents: write"))
        assertTrue(workflow.contains("apksigner"))
        assertFalse(workflow.contains("LOCKED_BOOT_COMPLETED"))
    }

    @Test
    fun `ci does not cancel pull request jobs when the same branch is pushed`() {
        val workflow = locate(".github/workflows/ci.yml").readText()
        assertTrue(workflow.contains("github.event.pull_request.number || github.ref"))
        assertFalse(workflow.contains("github.event.pull_request.head.ref || github.ref_name"))
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
