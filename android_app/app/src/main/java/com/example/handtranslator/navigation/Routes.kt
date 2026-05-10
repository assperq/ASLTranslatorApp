package com.example.handtranslator.navigation

sealed class Routes(val route: String) {
    data object MainScreen : Routes("main")
    data object Preferences : Routes("preferences")
}