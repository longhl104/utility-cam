package com.utility.cam.ui.capturereview

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
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
import com.utility.cam.data.PreferencesManager
import com.utility.cam.data.TTLDuration
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureReviewScreen(
    capturedImagePath: String,
    onPhotoSaved: () -> Unit,
    onRetake: () -> Unit
) {
    val context = LocalContext.current
    val storageManager = remember { PhotoStorageManager(context) }
    val preferencesManager = remember { PreferencesManager(context) }
    val coroutineScope = rememberCoroutineScope()
    
    val defaultTTL by preferencesManager.getDefaultTTL().collectAsState(initial = TTLDuration.TWENTY_FOUR_HOURS)
    
    var selectedTTL by remember { mutableStateOf<TTLDuration?>(null) }
    var description by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    
    val ttl = selectedTTL ?: defaultTTL
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review Photo") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Image preview
            val painter = rememberAsyncImagePainter(
                ImageRequest.Builder(context)
                    .data(File(capturedImagePath))
                    .crossfade(true)
                    .build()
            )
            
            Image(
                painter = painter,
                contentDescription = "Captured photo",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentScale = ContentScale.Fit
            )
            
            // Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // TTL Selection
                Text(
                    "Delete after:",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TTLDuration.values().forEach { duration ->
                        FilterChip(
                            selected = ttl == duration,
                            onClick = { selectedTTL = duration },
                            label = { Text(duration.displayName) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    placeholder = { Text("e.g., Parking Spot: Level 4") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onRetake,
                        modifier = Modifier.weight(1f),
                        enabled = !isSaving
                    ) {
                        Text("Retake")
                    }
                    
                    Button(
                        onClick = {
                            isSaving = true
                            coroutineScope.launch {
                                storageManager.savePhoto(
                                    imageFile = File(capturedImagePath),
                                    ttlDuration = ttl,
                                    description = description.takeIf { it.isNotBlank() }
                                )
                                isSaving = false
                                onPhotoSaved()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}
