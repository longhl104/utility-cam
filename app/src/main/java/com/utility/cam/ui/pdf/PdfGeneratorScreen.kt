package com.utility.cam.ui.pdf

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.utility.cam.R
import com.utility.cam.analytics.AnalyticsHelper
import com.utility.cam.data.PhotoStorageManager
import com.utility.cam.data.UtilityMedia
import com.utility.cam.ui.ads.BottomAdBanner
import com.utility.cam.ui.ads.AdUnitIds
import com.utility.cam.ui.common.rememberProUserState
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfGeneratorScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val storageManager = remember { PhotoStorageManager(context) }
    val scope = rememberCoroutineScope()
    val actualIsProUser = rememberProUserState()

    var availableMedia by remember { mutableStateOf<List<UtilityMedia>>(emptyList()) }
    var selectedMediaIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var isGenerating by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var generatedPdfUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val generationFailedMessage = stringResource(R.string.pdf_generation_failed)
    val noViewerMessage = stringResource(R.string.pdf_no_viewer)

    // Load available media
    LaunchedEffect(Unit) {
        AnalyticsHelper.logScreenView("PDF Generator", "PdfGeneratorScreen")
        availableMedia = storageManager.getAllPhotos().filter { media ->
            // Only include images (not videos)
            !media.filePath.endsWith(".mp4", ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.pdf_generator_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.bin_back)
                        )
                    }
                },
                actions = {
                    if (selectedMediaIds.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                isGenerating = true
                                scope.launch {
                                    try {
                                        // Use the ordered list to maintain selection order
                                        val selectedMedia = selectedMediaIds.mapNotNull { id ->
                                            availableMedia.find { it.id == id }
                                        }
                                        val pdfUri = generatePdf(context, selectedMedia)
                                        if (pdfUri != null) {
                                            AnalyticsHelper.logPdfGenerated(selectedMedia.size)
                                            generatedPdfUri = pdfUri
                                            showSuccessDialog = true
                                            selectedMediaIds = emptyList()
                                        } else {
                                            errorMessage = generationFailedMessage
                                            showErrorDialog = true
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = e.message ?: generationFailedMessage
                                        showErrorDialog = true
                                    } finally {
                                        isGenerating = false
                                    }
                                }
                            },
                            enabled = !isGenerating
                        ) {
                            Icon(
                                Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                stringResource(R.string.pdf_generate_button, selectedMediaIds.size),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            BottomAdBanner(
                isProUser = actualIsProUser,
                screenName = "PdfGenerator",
                adUnitId = AdUnitIds.BANNER_PDF_GENERATOR
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (availableMedia.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.pdf_no_images),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.pdf_no_images_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Instructions
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                stringResource(R.string.pdf_instructions),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                stringResource(R.string.pdf_instructions_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // Selected count
                    if (selectedMediaIds.isNotEmpty()) {
                        Text(
                            stringResource(R.string.pdf_selected_count, selectedMediaIds.size),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    // Image grid
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 120.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableMedia, key = { it.id }) { media ->
                            val selectionIndex = selectedMediaIds.indexOf(media.id)
                            ImageSelectionItem(
                                media = media,
                                isSelected = selectionIndex >= 0,
                                selectionNumber = if (selectionIndex >= 0) selectionIndex + 1 else null,
                                onClick = {
                                    selectedMediaIds = if (media.id in selectedMediaIds) {
                                        selectedMediaIds - media.id
                                    } else {
                                        selectedMediaIds + media.id
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Loading overlay
            if (isGenerating) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(stringResource(R.string.pdf_generating))
                        }
                    }
                }
            }
        }
    }

    // Success dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                generatedPdfUri = null
            },
            icon = {
                Icon(
                    Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text(stringResource(R.string.pdf_success_title)) },
            text = { Text(stringResource(R.string.pdf_success_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        generatedPdfUri?.let { uri ->
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "application/pdf")
                                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                                       android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                            }
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                // No PDF viewer installed
                                android.widget.Toast.makeText(
                                    context,
                                    noViewerMessage,
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        showSuccessDialog = false
                        generatedPdfUri = null
                    }
                ) {
                    Text(stringResource(R.string.pdf_view_button))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSuccessDialog = false
                        generatedPdfUri = null
                    }
                ) {
                    Text(stringResource(R.string.bin_cancel_button))
                }
            }
        )
    }

    // Error dialog
    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text(stringResource(R.string.pdf_error_title)) },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = false }) {
                    Text(stringResource(R.string.bin_cancel_button))
                }
            }
        )
    }
}

@Composable
private fun ImageSelectionItem(
    media: UtilityMedia,
    isSelected: Boolean,
    selectionNumber: Int?,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = if (isSelected) 3.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
    ) {
        val painter = rememberAsyncImagePainter(
            ImageRequest.Builder(LocalContext.current)
                .data(File(media.filePath))
                .crossfade(true)
                .build()
        )

        Image(
            painter = painter,
            contentDescription = media.description,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Selection overlay
        if (isSelected && selectionNumber != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            )

            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = selectionNumber.toString(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private suspend fun generatePdf(context: android.content.Context, selectedMedia: List<UtilityMedia>): android.net.Uri? {
    return try {
        withContext(Dispatchers.IO) {
            // Create PDF document
            val pdfDocument = android.graphics.pdf.PdfDocument()

            // Standard A4 page size in points (1 point = 1/72 inch)
            val pageWidth = 595 // A4 width in points
            val pageHeight = 842 // A4 height in points
            val margin = 40
            val availableWidth = pageWidth - (2 * margin)
            val availableHeight = pageHeight - (2 * margin)

            selectedMedia.forEachIndexed { index, media ->
                // Load bitmap
                val bitmap = android.graphics.BitmapFactory.decodeFile(media.filePath)
                    ?: return@withContext null

                // Calculate scaling to fit within available space while maintaining aspect ratio
                val bitmapWidth = bitmap.width.toFloat()
                val bitmapHeight = bitmap.height.toFloat()
                val scale = minOf(
                    availableWidth / bitmapWidth,
                    availableHeight / bitmapHeight
                )

                val scaledWidth = (bitmapWidth * scale).toInt()
                val scaledHeight = (bitmapHeight * scale).toInt()

                // Center the image on the page
                val xOffset = margin + (availableWidth - scaledWidth) / 2
                val yOffset = margin + (availableHeight - scaledHeight) / 2

                // Create a new page
                val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(
                    pageWidth,
                    pageHeight,
                    index + 1
                ).create()

                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                // Draw white background
                canvas.drawColor(android.graphics.Color.WHITE)

                // Draw the scaled bitmap
                val destRect = android.graphics.Rect(
                    xOffset,
                    yOffset,
                    xOffset + scaledWidth,
                    yOffset + scaledHeight
                )
                canvas.drawBitmap(bitmap, null, destRect, null)

                pdfDocument.finishPage(page)
                bitmap.recycle()
            }

            // Save PDF to Downloads folder
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                .format(Date())
            val fileName = "UtilityCam_$timestamp.pdf"

            var resultUri: android.net.Uri? = null

            // Use MediaStore for Android 10+ (API 29+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                }

                val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    resolver.openOutputStream(it)?.use { outputStream ->
                        pdfDocument.writeTo(outputStream)
                    }
                    resultUri = it
                }
            } else {
                // For older Android versions
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS
                )
                val pdfFile = File(downloadsDir, fileName)

                pdfFile.outputStream().use { outputStream ->
                    pdfDocument.writeTo(outputStream)
                }

                // Make the file visible in the Downloads app
                android.media.MediaScannerConnection.scanFile(
                    context,
                    arrayOf(pdfFile.absolutePath),
                    arrayOf("application/pdf"),
                    null
                )

                resultUri = android.net.Uri.fromFile(pdfFile)
            }

            pdfDocument.close()

            resultUri
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
