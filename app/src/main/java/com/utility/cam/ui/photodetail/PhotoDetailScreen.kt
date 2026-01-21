package com.utility.cam.ui.photodetail

import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.core.content.FileProvider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.utility.cam.R
import com.utility.cam.data.PhotoStorageManager
import com.utility.cam.data.UtilityPhoto
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.res.stringResource

@SuppressLint("LocalContextGetResourceValueCall")
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
                    title = { Text(stringResource(R.string.photo_detail_title)) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.photo_detail_back))
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                // Share photo using Android's share sheet
                                val photoFile = File(currentPhoto.filePath)
                                val photoUri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    photoFile
                                )

                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/jpeg"
                                    putExtra(Intent.EXTRA_STREAM, photoUri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    currentPhoto.description?.let {
                                        putExtra(Intent.EXTRA_TEXT, it)
                                    }
                                }

                                context.startActivity(
                                    Intent.createChooser(
                                        shareIntent,
                                        context.getString(R.string.photo_detail_share_title)
                                    )
                                )
                            }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = stringResource(R.string.photo_detail_share))
                        }
                        IconButton(
                            onClick = { showSaveDialog = true }
                        ) {
                            Icon(Icons.Default.Save, contentDescription = stringResource(R.string.photo_detail_save))
                        }
                        IconButton(
                            onClick = { showDeleteDialog = true }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.photo_detail_delete))
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
                                stringResource(R.string.photo_detail_expires_in, currentPhoto.getFormattedTimeRemaining()),
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
                        stringResource(R.string.photo_detail_captured),
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
                            stringResource(R.string.photo_detail_description),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = { showDescriptionDialog = true }) {
                            Text(stringResource(if (description.isEmpty()) R.string.photo_detail_add else R.string.photo_detail_edit))
                        }
                    }
                    
                    if (description.isNotEmpty()) {
                        Text(
                            description,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Text(
                            stringResource(R.string.photo_detail_no_description),
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
                        Text(stringResource(R.string.photo_detail_keep_forever))
                    }
                }
            }
        }
        
        // Delete confirmation dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(stringResource(R.string.photo_detail_delete_title)) },
                text = { Text(stringResource(R.string.photo_detail_delete_message)) },
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
                        Text(stringResource(R.string.photo_detail_delete_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text(stringResource(R.string.photo_detail_cancel))
                    }
                }
            )
        }
        
        // Save confirmation dialog
        if (showSaveDialog) {
            AlertDialog(
                onDismissRequest = { showSaveDialog = false },
                title = { Text(stringResource(R.string.photo_detail_save_title)) },
                text = { Text(stringResource(R.string.photo_detail_save_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                val success = storageManager.saveToGallery(photoId)
                                showSaveDialog = false
                                if (success) {
                                    snackbarMessage = context.getString(R.string.photo_detail_saved_success)
                                    kotlinx.coroutines.delay(1000)
                                    onNavigateBack()
                                } else {
                                    snackbarMessage = context.getString(R.string.photo_detail_save_failed)
                                }
                            }
                        }
                    ) {
                        Text(context.getString(R.string.photo_detail_save_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveDialog = false }) {
                        Text(context.getString(R.string.photo_detail_cancel))
                    }
                }
            )
        }
        
        // Description dialog
        if (showDescriptionDialog) {
            var tempDescription by remember { mutableStateOf(description) }
            
            AlertDialog(
                onDismissRequest = { showDescriptionDialog = false },
                title = { Text(context.getString(R.string.photo_detail_edit_description_title)) },
                text = {
                    OutlinedTextField(
                        value = tempDescription,
                        onValueChange = { tempDescription = it },
                        label = { Text(context.getString(R.string.photo_detail_description)) },
                        placeholder = { Text(context.getString(R.string.photo_detail_edit_description_hint)) },
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
                        Text(context.getString(R.string.photo_detail_save_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDescriptionDialog = false }) {
                        Text(context.getString(R.string.photo_detail_cancel))
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
