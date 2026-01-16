package com.utility.cam.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.utility.cam.MainActivity
import com.utility.cam.data.PhotoStorageManager
import com.utility.cam.data.UtilityPhoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Home screen widget showing active utility photos
 */
class UtilityCamWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val photos = withContext(Dispatchers.IO) {
            PhotoStorageManager(context).getAllPhotos()
        }

        provideContent {
            UtilityCamWidgetContent(photos)
        }
    }

    companion object {
        val PHOTO_ID_KEY = ActionParameters.Key<String>("photo_id")
    }
}

@Composable
fun UtilityCamWidgetContent(photos: List<UtilityPhoto>) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color(0xFF1C1B1F))
    ) {
        // Header - fixed at top
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(16.dp)
                .clickable(actionStartActivity<MainActivity>()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Utility Cam",
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        if (photos.isEmpty()) {
            // Empty state
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "No active photos",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFB0B0B0)),
                        fontSize = 14.sp
                    )
                )
                Spacer(modifier = GlanceModifier.height(8.dp))
                Text(
                    text = "Open app to capture",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF808080)),
                        fontSize = 12.sp
                    )
                )
            }
        } else {
            // Scrollable photo list
            LazyColumn(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight()
            ) {
                item {
                    Spacer(modifier = GlanceModifier.height(8.dp))
                }

                items(photos) { photo ->
                    Box(
                        modifier = GlanceModifier
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clickable(
                                actionStartActivity(
                                    activity = MainActivity::class.java,
                                    parameters = actionParametersOf(
                                        UtilityCamWidget.PHOTO_ID_KEY to photo.id
                                    )
                                )
                            )
                    ) {
                        WidgetPhotoItem(photo)
                    }
                }

                item {
                    Spacer(modifier = GlanceModifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun WidgetPhotoItem(photo: UtilityPhoto) {
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(Color(0xFF2C2B2F))
            .padding(12.dp)
    ) {
        Column {
            // Description or filename
            Text(
                text = photo.description ?: "Photo ${photo.id.take(8)}",
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1
            )

            Spacer(modifier = GlanceModifier.height(4.dp))

            // Expiration time
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⏱ Expires in: ",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFB0B0B0)),
                        fontSize = 12.sp
                    )
                )
                Text(
                    text = photo.getFormattedTimeRemaining(),
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFFF6B6B)),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

class UtilityCamWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = UtilityCamWidget()
}
