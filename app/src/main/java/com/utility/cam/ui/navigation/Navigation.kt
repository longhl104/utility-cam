package com.utility.cam.ui.navigation

import android.net.Uri
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.utility.cam.ui.camera.CameraScreen
import com.utility.cam.ui.capturereview.CaptureReviewScreen
import com.utility.cam.ui.gallery.GalleryScreen
import com.utility.cam.ui.mediadetail.MediaDetailScreen
import com.utility.cam.ui.permissions.NotificationPermissionHandler
import com.utility.cam.ui.settings.SettingsScreen
import com.utility.cam.ui.pro.ProScreen
import com.utility.cam.ui.bin.BinScreen
import com.utility.cam.ui.pdf.PdfGeneratorScreen
import kotlinx.coroutines.launch

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
    object MediaDetail : Screen("media_detail/{mediaId}") {
        fun createRoute(mediaId: String) = "media_detail/$mediaId"
    }
    object Settings : Screen("settings")
    object Pro : Screen("pro")
    object Bin : Screen("bin")
    object PdfGenerator : Screen("pdf_generator")
}

@Composable
fun UtilityCamNavigation(initialPhotoId: String? = null) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    // Request notification permission on first launch
    NotificationPermissionHandler()

    // Navigate to media detail if mediaId is provided from widget
    LaunchedEffect(initialPhotoId) {
        if (initialPhotoId != null) {
            navController.navigate(Screen.MediaDetail.createRoute(initialPhotoId))
        }
    }

    // Function to open drawer
    val openDrawer: () -> Unit = {
        scope.launch {
            drawerState.open()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppNavigationDrawer(
                currentRoute = currentRoute,
                onNavigateToGallery = {
                    navController.navigate(Screen.Gallery.route) {
                        popUpTo(Screen.Gallery.route) { inclusive = true }
                    }
                },
                onNavigateToBin = {
                    navController.navigate(Screen.Bin.route)
                },
                onNavigateToPdfGenerator = {
                    navController.navigate(Screen.PdfGenerator.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToPro = {
                    navController.navigate(Screen.Pro.route)
                },
                onDismiss = {
                    scope.launch {
                        drawerState.close()
                    }
                }
            )
        }
    ) {
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
                    onNavigateToMediaDetail = { mediaId ->
                        navController.navigate(Screen.MediaDetail.createRoute(mediaId))
                    },
                    onNavigateToBin = {
                        navController.navigate(Screen.Bin.route)
                    },
                    onOpenDrawer = openDrawer
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
                },
                onNavigateToPro = {
                    navController.navigate(Screen.Pro.route)
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
                    onMediaSaved = {
                        navController.popBackStack(Screen.Gallery.route, inclusive = false)
                    },
                    onRetake = {
                        navController.popBackStack()
                        navController.navigate(Screen.Camera.createRoute(mode))
                    },
                    onNavigateToPro = {
                        navController.navigate(Screen.Pro.route)
                    },
                    onNavigateToGallery = {
                        navController.popBackStack(Screen.Gallery.route, inclusive = false)
                    }
                )
            }
        }

        composable(Screen.MediaDetail.route) { backStackEntry ->
            val mediaId = backStackEntry.arguments?.getString("mediaId")

            mediaId?.let {
                MediaDetailScreen(
                    mediaId = it,
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

        composable(Screen.Bin.route) {
            BinScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.PdfGenerator.route) {
            PdfGeneratorScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        }
    }
}
