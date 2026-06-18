package com.h2grow.skat_load_cell.navigation

import androidx.compose.runtime.mutableStateListOf

class Router {
    private val _backStack = mutableStateListOf<Route>(Route.Main)
    val backStack: List<Route> get() = _backStack

    val current: Route get() = _backStack.last()

    fun push(route: Route) {
        _backStack.add(route)
    }

    fun pop() {
        if (_backStack.size > 1) {
            _backStack.removeAt(_backStack.lastIndex)
        }
    }
}