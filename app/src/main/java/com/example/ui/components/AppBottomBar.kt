package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.ui.navigation.AppScreen

@Composable
fun AppBottomBar(
    currentScreen: AppScreen,
    onNavigate: (AppScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        Triple(AppScreen.HOME, "Home", Icons.Default.Home),
        Triple(AppScreen.HISTORY, "History", Icons.Default.History),
        Triple(AppScreen.FAVORITES, "Favorites", Icons.Default.Favorite),
        Triple(AppScreen.MY_QR, "My QR", Icons.Default.Person)
    )

    NavigationBar(modifier = modifier.testTag("app_bottom_bar")) {
        items.forEach { (screen, title, icon) ->
            val isSelected = currentScreen == screen
            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(screen) },
                icon = { Icon(icon, contentDescription = title) },
                label = { Text(title) },
                modifier = Modifier.testTag("bottom_nav_${screen.name.lowercase()}")
            )
        }
    }
}
