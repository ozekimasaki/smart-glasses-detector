package jp.smartglasses.detector.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import jp.smartglasses.detector.R
import jp.smartglasses.detector.presentation.navigation.Screen

@Composable
fun BottomNavigationBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = currentRoute == Screen.Main.route,
            onClick = { onNavigate(Screen.Main.route) },
            icon = {
                if (currentRoute == Screen.Main.route) {
                    Icon(Icons.Filled.Home, contentDescription = stringResource(R.string.nav_home_desc))
                } else {
                    Icon(Icons.Outlined.Home, contentDescription = stringResource(R.string.nav_home_desc))
                }
            },
            label = { Text(stringResource(R.string.nav_home)) }
        )
        NavigationBarItem(
            selected = currentRoute == Screen.History.route,
            onClick = { onNavigate(Screen.History.route) },
            icon = {
                if (currentRoute == Screen.History.route) {
                    Icon(Icons.Filled.History, contentDescription = stringResource(R.string.nav_history_desc))
                } else {
                    Icon(Icons.Outlined.History, contentDescription = stringResource(R.string.nav_history_desc))
                }
            },
            label = { Text(stringResource(R.string.nav_history)) }
        )
        NavigationBarItem(
            selected = currentRoute == Screen.Settings.route,
            onClick = { onNavigate(Screen.Settings.route) },
            icon = {
                if (currentRoute == Screen.Settings.route) {
                    Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.nav_settings_desc))
                } else {
                    Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.nav_settings_desc))
                }
            },
            label = { Text(stringResource(R.string.nav_settings)) }
        )
    }
}
