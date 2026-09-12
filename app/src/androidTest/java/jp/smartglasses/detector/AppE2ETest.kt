package jp.smartglasses.detector

import android.Manifest
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppE2ETest {
    private val composeRule = createAndroidComposeRule<MainActivity>()
    private val permissionRule = GrantPermissionRule.grant(*requiredPermissions())

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(permissionRule).around(composeRule)

    @Test
    fun onboardingThenMainHistorySettingsAboutAndPrivacy() {
        val next = string(R.string.next)

        waitForText(string(R.string.onboarding_welcome))
        composeRule.onNodeWithText(string(R.string.onboarding_welcome)).assertIsDisplayed()
        composeRule.onNodeWithText(next).performClick()

        waitForText(string(R.string.onboarding_howto))
        composeRule.onNodeWithText(next).performClick()

        waitForText(string(R.string.onboarding_permissions))
        composeRule.onNodeWithText(string(R.string.onboarding_grant))
            .performScrollTo()
            .performClick()

        waitForText(string(R.string.main_start_button))
        composeRule.onNodeWithText(string(R.string.main_app_subtitle)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.main_start_button)).assertIsDisplayed()

        composeRule.onNodeWithText(string(R.string.nav_history)).performClick()
        waitForText(string(R.string.history_empty_title))
        composeRule.onNodeWithText(string(R.string.history_empty_message)).assertIsDisplayed()

        composeRule.onNodeWithText(string(R.string.nav_settings)).performClick()
        waitForText(string(R.string.settings_group_sensitivity))
        composeRule.onNodeWithText(string(R.string.settings_notification))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.settings_sensitivity_balanced))
            .performScrollTo()
            .assertIsDisplayed()

        composeRule.onNodeWithText(string(R.string.settings_about))
            .performScrollTo()
            .performClick()
        waitForText(string(R.string.about_version))
        composeRule.onNodeWithText(string(R.string.about_title)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(string(R.string.about_close)).performClick()

        waitForText(string(R.string.settings_group_sensitivity))
        composeRule.onNodeWithText(string(R.string.settings_privacy))
            .performScrollTo()
            .performClick()
        waitForText(string(R.string.privacy_open_full_policy))
        composeRule.onNodeWithText(string(R.string.privacy_contact)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(string(R.string.privacy_close)).performClick()

        composeRule.onNodeWithText(string(R.string.nav_home)).performClick()
        waitForText(string(R.string.main_start_button))
        composeRule.onNodeWithText(string(R.string.main_start_button)).assertIsDisplayed()
    }

    private fun string(resId: Int): String = composeRule.activity.getString(resId)

    private fun waitForText(text: String, timeoutMs: Long = 30_000) {
        composeRule.waitUntil(timeoutMs) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    companion object {
        private fun requiredPermissions(): Array<String> {
            val permissions = mutableListOf<String>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permissions += Manifest.permission.BLUETOOTH_SCAN
                permissions += Manifest.permission.BLUETOOTH_CONNECT
            } else {
                permissions += Manifest.permission.ACCESS_FINE_LOCATION
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions += Manifest.permission.POST_NOTIFICATIONS
            }
            return permissions.toTypedArray()
        }
    }
}
