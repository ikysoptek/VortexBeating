/**
 * NanzBeatles Project (C) 2026
 */

package com.ikyvortex.vortexmusic.ui.utils

import androidx.navigation.NavController
import com.ikyvortex.vortexmusic.ui.screens.Screens

fun NavController.backToMain() {
    val mainRoutes = Screens.MainScreens.mapNotNull { it?.route }

    while (previousBackStackEntry != null &&
        currentBackStackEntry?.destination?.route !in mainRoutes
    ) {
        popBackStack()
    }
}
