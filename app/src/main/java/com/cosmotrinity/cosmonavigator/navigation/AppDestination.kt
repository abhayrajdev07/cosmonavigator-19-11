
package com.cosmotrinity.cosmonavigator.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppDestination(
    val title: String,
    val icon: ImageVector
) {
    HOME("Home", Icons.Filled.Home),
    PLANNER("Planner", Icons.Filled.Edit),
    CALENDAR("Calendar", Icons.Filled.DateRange),
    PROGRESS("Progress", Icons.Filled.BarChart),
    MORE("More", Icons.Filled.Menu)
}
