package jp.smartglasses.detector.data.bluetooth

import android.Manifest
import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanPermissionAppOpsTest {
    @Test
    fun `android 12 plus watches bluetooth scan and connect`() {
        assertEquals(
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            ),
            ScanPermissionAppOps.permissionsToWatch(Build.VERSION_CODES.S)
        )
        assertFalse(ScanPermissionAppOps.usesFineLocationOp(Build.VERSION_CODES.S))
    }

    @Test
    fun `android 11 uses the fine location app op`() {
        assertTrue(ScanPermissionAppOps.usesFineLocationOp(Build.VERSION_CODES.R))
        assertTrue(ScanPermissionAppOps.permissionsToWatch(Build.VERSION_CODES.R).isEmpty())
    }
}
