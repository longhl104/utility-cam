package com.utility.cam.ui.photodetail

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.utility.cam.data.PhotoStorageManager
import com.utility.cam.data.UtilityPhoto
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoDetailScreen(
    photoId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val storageManager = remember { PhotoStorageManager(context) }
    val coroutineScope = rememberCoroutineScope()
    
    var photo by remember { mutableStateOf<UtilityPhoto?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf("") }
    var showDescriptionDialog by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(photoId) {
        photo = storageManager.getPhoto(photoId)
        description = photo?.description ?: ""
    }
    
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }
    
    photo?.let { currentPhoto ->
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Photo Details") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showSaveDialog = true }
                        ) {
                            Icon(Icons.Default.Save, contentDescription = "Save to Gallery")
                        }
                        IconButton(
                            onClick = { showDeleteDialog = true }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                // Image
                val painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(context)
                        .data(File(currentPhoto.filePath))
                        .crossfade(true)
                        .build()
                )
                
                Image(
                    painter = painter,
                    contentDescription = currentPhoto.description ?: "Utility photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f),
                    contentScale = ContentScale.Fit
                )
                
                // Details
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Expiration info
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Expires in ${currentPhoto.getFormattedTimeRemaining()}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
                                    .format(Date(currentPhoto.expirationTimestamp)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Captured time
                    Text(
                        "Captured",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
                            .format(Date(currentPhoto.captureTimestamp)),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Description
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Description",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = { showDescriptionDialog = true }) {
                            Text(if (description.isEmpty()) "Add" else "Edit")
                        }
                    }
                    
                    if (description.isNotEmpty()) {
                        Text(
                            description,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Text(
                            "No description",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Actions
                    Button(
                        onClick = { showSaveDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Keep Forever (Save to Gallery)")
                    }
                }
            }
        }
        
        // Delete confirmation dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Photo?") },
                text = { Text("This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                storageManager.deletePhoto(photoId)
                                showDeleteDialog = false
                                onNavigateBack()
                            }
                        }
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
        
        // Save confirmation dialog
        if (showSaveDialog) {
            AlertDialog(
                onDismissRequest = { showSaveDialog = false },
                title = { Text("Save to Gallery?") },
                text = { Text("This photo will be moved to your main gallery and will no longer expire.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                val success = storageManager.saveToGallery(photoId)
                                showSaveDialog = false
                                if (success) {
                                    snackbarMessage = "Saved to gallery"
                                    kotlinx.coroutines.delay(1000)
                                    onNavigateBack()
                                } else {
                                    snackbarMessage = "Failed to save"
                                }
                            }
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
        
        // Description dialog
        if (showDescriptionDialog) {
            var tempDescription by remember { mutableStateOf(description) }
            
            AlertDialog(
                onDismissRequest = { showDescriptionDialog = false },
                title = { Text("Add Description") },
                text = {
                    OutlinedTextField(
                        value = tempDescription,
                        onValueChange = { tempDescription = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                storageManager.updateDescription(photoId, tempDescription)
                                description = tempDescription
                                photo = storageManager.getPhoto(photoId)
                                showDescriptionDialog = false
                            }
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDescriptionDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    } ?: Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
