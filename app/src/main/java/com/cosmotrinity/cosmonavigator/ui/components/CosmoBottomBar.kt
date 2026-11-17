
package com.cosmotrinity.cosmonavigator.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cosmotrinity.cosmonavigator.navigation.AppDestination

@Composable
fun CosmoBottomBar(
    selectedDestination: AppDestination,
    onDestinationSelected: (AppDestination) -> Unit
) {
    NavigationBar(
        containerColor = Color(0xFF151D32),
        contentColor = Color.White,
        tonalElevation = 0.dp
    ) {
        AppDestination.entries.forEach { destination ->

            val selected = selectedDestination == destination

            NavigationBarItem(
                selected = selected,
                onClick = {
                    onDestinationSelected(destination)
                },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = destination.title
                    )
                },
                label = {
                    Text(destination.title)
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF8B7CFF),
                    selectedTextColor = Color(0xFF8B7CFF),
                    indicatorColor = Color(0xFF303052),
                    unselectedIconColor = Color(0xFFA4AEC4),
                    unselectedTextColor = Color(0xFFA4AEC4)
                )
            )
        }
    }
}
