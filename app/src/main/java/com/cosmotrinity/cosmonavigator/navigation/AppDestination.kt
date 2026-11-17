
package com.cosmotrinity.cosmonavigator.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppDestination(
    val title: String,
    val icon: ImageVector
) {
    HOME("Home", Icons.Filled.Home),
    PLANNER("Planner", Icons.Filled.Edit),
    CALENDAR("Calendar", Icons.Filled.DateRange),
    VAULT("Vault", Icons.Filled.FolderOpen),
    MORE("More", Icons.Filled.Menu)
}
