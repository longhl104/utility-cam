package com.utility.cam.ui.navigation

import android.net.Uri
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.utility.cam.ui.camera.CameraScreen
import com.utility.cam.ui.capturereview.CaptureReviewScreen
import com.utility.cam.ui.gallery.GalleryScreen
import com.utility.cam.ui.photodetail.PhotoDetailScreen
import com.utility.cam.ui.permissions.NotificationPermissionHandler
import com.utility.cam.ui.settings.SettingsScreen
import com.utility.cam.ui.pro.ProScreen

sealed class Screen(val route: String) {
    object Gallery : Screen("gallery")
    object Camera : Screen("camera?mode={mode}") {
        fun createRoute(mode: String = "photo"): String {
            return "camera?mode=$mode"
        }
    }
    object CaptureReview : Screen("capture_review?imagePath={imagePath}&mode={mode}") {
        fun createRoute(imagePath: String, mode: String = "photo"): String {
            val encodedPath = Uri.encode(imagePath)
            return "capture_review?imagePath=$encodedPath&mode=$mode"
        }
    }
    object PhotoDetail : Screen("photo_detail/{photoId}") {
        fun createRoute(photoId: String) = "photo_detail/$photoId"
    }
    object Settings : Screen("settings")
    object Pro : Screen("pro")
}

@Composable
fun UtilityCamNavigation(initialPhotoId: String? = null) {
    val navController = rememberNavController()

    // Request notification permission on first launch
    NotificationPermissionHandler()

    // Navigate to photo detail if photoId is provided from widget
    LaunchedEffect(initialPhotoId) {
        if (initialPhotoId != null) {
            navController.navigate(Screen.PhotoDetail.createRoute(initialPhotoId))
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Gallery.route
    ) {
        composable(Screen.Gallery.route) {
            GalleryScreen(
                onNavigateToCamera = {
                    navController.navigate(Screen.Camera.createRoute())
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToPhotoDetail = { photoId ->
                    navController.navigate(Screen.PhotoDetail.createRoute(photoId))
                }
            )
        }

        composable(Screen.Camera.route) { backStackEntry ->
            val initialMode = backStackEntry.arguments?.getString("mode") ?: "photo"
            CameraScreen(
                initialMode = initialMode,
                onPhotoCapture = { imageFile, currentMode ->
                    navController.navigate(
                        Screen.CaptureReview.createRoute(imageFile.absolutePath, currentMode)
                    )
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.CaptureReview.route) { backStackEntry ->
            val encodedImagePath = backStackEntry.arguments?.getString("imagePath")
            val imagePath = encodedImagePath?.let { Uri.decode(it) }
            val mode = backStackEntry.arguments?.getString("mode") ?: "photo"

            imagePath?.let {
                CaptureReviewScreen(
                    capturedImagePath = it,
                    onPhotoSaved = {
                        navController.popBackStack(Screen.Gallery.route, inclusive = false)
                    },
                    onRetake = {
                        navController.popBackStack()
                        navController.navigate(Screen.Camera.createRoute(mode))
                    }
                )
            }
        }

        composable(Screen.PhotoDetail.route) { backStackEntry ->
            val photoId = backStackEntry.arguments?.getString("photoId")

            photoId?.let {
                PhotoDetailScreen(
                    photoId = it,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToPro = {
                    navController.navigate(Screen.Pro.route)
                }
            )
        }

        composable(Screen.Pro.route) {
            ProScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
