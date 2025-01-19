package net.maxsmr.core.ui.compose.components

import androidx.compose.material.SnackbarHostState
import androidx.navigation.NavHostController

class ComposableDependencies(
    val navHostController: NavHostController,
    val snackbarHostState: SnackbarHostState
)