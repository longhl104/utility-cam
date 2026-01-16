package com.utility.cam.ui.navigation

import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.utility.cam.ui.camera.CameraScreen
import com.utility.cam.ui.capturereview.CaptureReviewScreen
import com.utility.cam.ui.gallery.GalleryScreen
import com.utility.cam.ui.photodetail.PhotoDetailScreen
import com.utility.cam.ui.settings.SettingsScreen

sealed class Screen(val route: String) {
    object Gallery : Screen("gallery")
    object Camera : Screen("camera")
    object CaptureReview : Screen("capture_review/{imagePath}") {
        fun createRoute(imagePath: String) = "capture_review/$imagePath"
    }
    object PhotoDetail : Screen("photo_detail/{photoId}") {
        fun createRoute(photoId: String) = "photo_detail/$photoId"
    }
    object Settings : Screen("settings")
}

@Composable
fun UtilityCamNavigation() {
    val navController = rememberNavController()
    var capturedImagePath by remember { mutableStateOf<String?>(null) }
    
    NavHost(
        navController = navController,
        startDestination = Screen.Gallery.route
    ) {
        composable(Screen.Gallery.route) {
            GalleryScreen(
                onNavigateToCamera = {
                    navController.navigate(Screen.Camera.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToPhotoDetail = { photoId ->
                    navController.navigate(Screen.PhotoDetail.createRoute(photoId))
                }
            )
        }
        
        composable(Screen.Camera.route) {
            CameraScreen(
                onPhotoCapture = { imageFile ->
                    capturedImagePath = imageFile.absolutePath
                    navController.navigate(
                        Screen.CaptureReview.createRoute(imageFile.absolutePath)
                    )
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.CaptureReview.route) { backStackEntry ->
            val imagePath = backStackEntry.arguments?.getString("imagePath")
            
            imagePath?.let {
                CaptureReviewScreen(
                    capturedImagePath = it,
                    onPhotoSaved = {
                        navController.popBackStack(Screen.Gallery.route, inclusive = false)
                    },
                    onRetake = {
                        navController.popBackStack()
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
                }
            )
        }
    }
}
