package com.utility.cam.ui.settings

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import com.utility.cam.BuildConfig
import com.utility.cam.R
import com.utility.cam.analytics.AnalyticsHelper
import com.utility.cam.data.FeedbackManager
import com.utility.cam.data.LocaleManager
import com.utility.cam.data.NotificationHelper
import com.utility.cam.data.PreferencesManager
import com.utility.cam.data.TTLDuration
import com.utility.cam.ui.common.rememberProUserStateWithManagers
import com.utility.cam.ui.permissions.isNotificationPermissionGranted
import com.utility.cam.worker.PhotoCleanupWorker
import kotlinx.coroutines.launch
import java.util.Locale

@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPro: () -> Unit = {}
) {
    val context = LocalContext.current
    val proUserState = rememberProUserStateWithManagers()
    val preferencesManager = proUserState.preferencesManager
    val actualIsProUser = proUserState.actualIsProUser

    val debugProOverride by preferencesManager.getDebugProOverride().collectAsState(initial = false)

    val localeManager = remember { LocaleManager(context) }
    val feedbackManager = remember { FeedbackManager(context) }
    val coroutineScope = rememberCoroutineScope()


    // SplitInstallManager for downloading language resources
    val splitInstallManager = remember { SplitInstallManagerFactory.create(context) }
    var isDownloadingLanguage by remember { mutableStateOf(false) }

    val defaultTTL by preferencesManager.getDefaultTTL()
        .collectAsState(initial = TTLDuration.TWENTY_FOUR_HOURS)
    val notificationsEnabled by preferencesManager.getNotificationsEnabled()
        .collectAsState(initial = true)
    val reminderNotificationsEnabled by preferencesManager.getReminderNotificationsEnabled()
        .collectAsState(initial = true)
    val analyticsEnabled by preferencesManager.getAnalyticsEnabled().collectAsState(initial = true)
    val hasNotificationPermission = isNotificationPermissionGranted()
    val cleanupDelaySeconds by preferencesManager.getCleanupDelaySeconds()
        .collectAsState(initial = 10)
    val selectedLanguage by localeManager.getSelectedLanguage()
        .collectAsState(initial = LocaleManager.SYSTEM_DEFAULT)
    val themeMode by preferencesManager.getThemeMode()
        .collectAsState(initial = PreferencesManager.THEME_MODE_SYSTEM)

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    val supportedLanguages = remember { localeManager.getSupportedLanguages() }

    var cleanupDelayInput by remember { mutableStateOf("") }
    var hasUserEdited by remember { mutableStateOf(false) }

    // Initialize input with current value from preferences
    LaunchedEffect(cleanupDelaySeconds) {
        // Only update if user hasn't edited the field yet
        if (!hasUserEdited) {
            cleanupDelayInput = cleanupDelaySeconds.toString()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(stringResource(R.string.settings_title))
                        if (actualIsProUser) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    "PRO",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Language Selection
            Text(
                stringResource(R.string.settings_language),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                stringResource(R.string.settings_language_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedCard(
                onClick = { showLanguageDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    val currentLanguage = supportedLanguages.find { it.code == selectedLanguage }
                        ?: supportedLanguages.first()

                    Column {
                        Text(
                            currentLanguage.displayName,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            stringResource(R.string.settings_language_restart_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text("▼", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            HorizontalDivider()

            Spacer(modifier = Modifier.height(16.dp))

            // Theme Selection
            Text(
                stringResource(R.string.settings_theme),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                stringResource(R.string.settings_theme_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedCard(
                onClick = { showThemeDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    val themeName = when (themeMode) {
                        PreferencesManager.THEME_MODE_LIGHT -> stringResource(R.string.settings_theme_light)
                        PreferencesManager.THEME_MODE_DARK -> stringResource(R.string.settings_theme_dark)
                        else -> stringResource(R.string.settings_theme_system)
                    }

                    Text(
                        themeName,
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Text("▼", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            HorizontalDivider()

            Spacer(modifier = Modifier.height(16.dp))

            // Pro Status Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (actualIsProUser)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToPro() }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (actualIsProUser) "✨ Pro User" else stringResource(R.string.settings_go_pro),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                color = if (actualIsProUser)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                if (actualIsProUser) stringResource(R.string.settings_pro_thank_you) else stringResource(
                                    R.string.settings_go_pro_hint
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (actualIsProUser)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = if (actualIsProUser)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            HorizontalDivider()

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                stringResource(R.string.settings_default_expiration),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                stringResource(R.string.settings_default_expiration_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            TTLDuration.entries
                .filter { !it.isDebugOnly || BuildConfig.DEBUG }
                .forEach { duration ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        RadioButton(
                            selected = defaultTTL == duration,
                            onClick = {
                                coroutineScope.launch {
                                    preferencesManager.setDefaultTTL(duration)
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            duration.getDisplayName(context),
                            modifier = Modifier.padding(top = 12.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

            Spacer(modifier = Modifier.height(32.dp))

            HorizontalDivider()

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                stringResource(R.string.settings_notifications),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.settings_cleanup_notifications),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.settings_cleanup_notifications_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = { enabled ->
                        coroutineScope.launch {
                            preferencesManager.setNotificationsEnabled(enabled)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.settings_reminder_notifications),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.settings_reminder_notifications_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = reminderNotificationsEnabled,
                    onCheckedChange = { enabled ->
                        coroutineScope.launch {
                            preferencesManager.setReminderNotificationsEnabled(enabled)
                        }
                    }
                )
            }

            // Show permission button if notifications are enabled but permission not granted
            if (notificationsEnabled && !hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            stringResource(R.string.settings_notification_permission_required),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.settings_notification_permission_message),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                // Open app settings
                                val intent =
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(stringResource(R.string.settings_open_settings))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            HorizontalDivider()

            Spacer(modifier = Modifier.height(16.dp))

            // Privacy Section
            Text(
                stringResource(R.string.settings_privacy),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.settings_analytics_consent),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.settings_analytics_consent_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = analyticsEnabled,
                    onCheckedChange = { enabled ->
                        coroutineScope.launch {
                            preferencesManager.setAnalyticsEnabled(enabled)
                            // Update Firebase Analytics collection
                            AnalyticsHelper.setAnalyticsEnabled(enabled)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            HorizontalDivider()

            Spacer(modifier = Modifier.height(16.dp))

            // Debug section (only visible in debug builds)
            if (BuildConfig.DEBUG) {
                Text(
                    stringResource(R.string.settings_debug_tools),
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    stringResource(R.string.settings_debug_tools_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Cleanup delay input
                OutlinedTextField(
                    value = cleanupDelayInput,
                    onValueChange = { newValue ->
                        // Mark as edited by user
                        hasUserEdited = true

                        // Only allow digits
                        if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                            cleanupDelayInput = newValue
                            // Update preference if valid
                            newValue.toIntOrNull()?.let { seconds ->
                                if (seconds in 1..3600) { // Max 1 hour
                                    coroutineScope.launch {
                                        preferencesManager.setCleanupDelaySeconds(seconds)
                                        // Reset the flag after saving so next restart shows correct value
                                        hasUserEdited = false
                                        Toast.makeText(
                                            context,
                                            context.getString(
                                                R.string.settings_cleanup_delay_updated,
                                                seconds
                                            ),
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        }
                    },
                    label = { Text(stringResource(R.string.settings_cleanup_delay_label)) },
                    supportingText = {
                        Text(stringResource(R.string.settings_cleanup_delay_hint))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = cleanupDelayInput.toIntOrNull()?.let { it !in 1..3600 } ?: false
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Debug Pro Override Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Override Pro Status",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Enable Pro features for testing without purchase",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = debugProOverride,
                        onCheckedChange = { enabled ->
                            coroutineScope.launch {
                                preferencesManager.setDebugProOverride(enabled)
                                Toast.makeText(
                                    context,
                                    if (enabled) "Pro features enabled" else "Pro features disabled",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        // Trigger immediate cleanup
                        Toast.makeText(
                            context,
                            context.getString(R.string.settings_trigger_cleanup_started),
                            Toast.LENGTH_SHORT
                        ).show()
                        val cleanupRequest = OneTimeWorkRequestBuilder<PhotoCleanupWorker>().build()
                        WorkManager.getInstance(context).enqueue(cleanupRequest)

                        // Observe the work status
                        WorkManager.getInstance(context)
                            .getWorkInfoByIdLiveData(cleanupRequest.id)
                            .observeForever { workInfo ->
                                when (workInfo?.state) {
                                    WorkInfo.State.SUCCEEDED -> {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.settings_cleanup_completed),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }

                                    WorkInfo.State.FAILED -> {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.settings_cleanup_failed),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }

                                    WorkInfo.State.RUNNING -> {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.settings_cleanup_running),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }

                                    else -> {}
                                }
                            }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.settings_trigger_cleanup))
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        // Test notification directly
                        Toast.makeText(
                            context,
                            context.getString(R.string.settings_test_notification_sent),
                            Toast.LENGTH_SHORT
                        ).show()
                        NotificationHelper.sendPhotoCleanupNotification(context, 1)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.settings_test_notification))
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        // Check WorkManager status
                        val workManager = WorkManager.getInstance(context)
                        val workInfos = workManager.getWorkInfosForUniqueWork("photo_cleanup").get()

                        if (workInfos.isEmpty()) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.settings_worker_not_scheduled),
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            val workInfo = workInfos[0]
                            val status = context.getString(
                                R.string.settings_worker_status_format,
                                workInfo.state.toString(),
                                workInfo.runAttemptCount,
                                workInfo.tags.joinToString()
                            )
                            Toast.makeText(context, status, Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.settings_check_worker_status))
                }

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider()

                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(
                stringResource(R.string.settings_about),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                stringResource(R.string.settings_about_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                stringResource(R.string.settings_about_feature_1),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                stringResource(R.string.settings_about_feature_2),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        stringResource(R.string.settings_about_privacy),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        stringResource(R.string.settings_warning_uninstall),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Send Feedback Button
            Button(
                onClick = {
                    val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                        data = "mailto:".toUri()
                        putExtra(
                            Intent.EXTRA_EMAIL,
                            arrayOf("longcode10041998@gmail.com")
                        ) // Replace with your email
                        putExtra(
                            Intent.EXTRA_SUBJECT,
                            context.getString(
                                R.string.settings_feedback_email_subject,
                                BuildConfig.VERSION_NAME
                            )
                        )
                        putExtra(
                            Intent.EXTRA_TEXT,
                            context.getString(
                                R.string.settings_feedback_email_body,
                                BuildConfig.VERSION_NAME,
                                BuildConfig.VERSION_CODE,
                                Build.VERSION.SDK_INT,
                                Build.MANUFACTURER,
                                Build.MODEL
                            )
                        )
                    }
                    try {
                        context.startActivity(
                            Intent.createChooser(
                                emailIntent,
                                context.getString(R.string.settings_feedback_chooser_title)
                            )
                        )
                    } catch (_: Exception) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.settings_feedback_no_email_app),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_send_feedback))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Love Utility Cam? Button - Triggers Google Play In-App Review
            Button(
                onClick = {
                    val activity = context as? Activity
                    if (activity != null) {
                        Log.d("SettingsScreen", "Manual review request from button")
                        val reviewManager =
                            com.google.android.play.core.review.ReviewManagerFactory.create(
                                context
                            )
                        val request = reviewManager.requestReviewFlow()

                        request.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Log.d(
                                    "SettingsScreen",
                                    "Review request successful, launching flow"
                                )
                                val reviewInfo = task.result
                                val flow = reviewManager.launchReviewFlow(activity, reviewInfo)

                                flow.addOnCompleteListener {
                                    Log.d("SettingsScreen", "Review flow completed")
                                    // Mark that user has rated (same as FeedbackDialog)
                                    coroutineScope.launch {
                                        feedbackManager.markUserRated()
                                    }
                                    // Note: In-App Review might not show in debug/test environments
                                    // If nothing appeared, open Play Store as fallback
                                    if (BuildConfig.DEBUG) {
                                        Log.w("SettingsScreen", "Debug build - In-App Review may not work, opening Play Store")
                                        openPlayStoreForReview(context)
                                    }
                                }

                                flow.addOnFailureListener { exception ->
                                    Log.e(
                                        "SettingsScreen",
                                        "Review flow failed: ${exception.message}"
                                    )
                                    Toast.makeText(
                                        context,
                                        "Opening Play Store...",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    openPlayStoreForReview(context)
                                }
                            } else {
                                Log.e(
                                    "SettingsScreen",
                                    "Review request failed: ${task.exception?.message}"
                                )
                                Toast.makeText(
                                    context,
                                    "Opening Play Store...",
                                    Toast.LENGTH_SHORT
                                ).show()
                                openPlayStoreForReview(context)
                            }
                        }

                        request.addOnFailureListener { exception ->
                            Log.e(
                                "SettingsScreen",
                                "Review request exception: ${exception.message}"
                            )
                            Toast.makeText(
                                context,
                                "Opening Play Store...",
                                Toast.LENGTH_SHORT
                            ).show()
                            openPlayStoreForReview(context)
                        }
                    } else {
                        Log.e("SettingsScreen", "No activity context available")
                        // Direct fallback if no activity
                        openPlayStoreForReview(context)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_love_utility_cam))
            }

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider()

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                stringResource(R.string.settings_app_version),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                stringResource(
                    R.string.settings_version_format,
                    BuildConfig.VERSION_NAME,
                    BuildConfig.VERSION_CODE
                ),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }

    // Language selection dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.settings_language)) },
            text = {
                Column {
                    supportedLanguages.forEach { language ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (!isDownloadingLanguage) {
                                        coroutineScope.launch {
                                            // Track analytics
                                            AnalyticsHelper.logLanguageChanged(
                                                oldLanguage = selectedLanguage,
                                                newLanguage = language.code
                                            )

                                            // Save language preference
                                            localeManager.setSelectedLanguage(language.code)

                                            // Download language resources if not system default
                                            if (language.code != LocaleManager.SYSTEM_DEFAULT) {
                                                isDownloadingLanguage = true
                                                val locale = Locale.forLanguageTag(language.code)

                                                val request = SplitInstallRequest.newBuilder()
                                                    .addLanguage(locale)
                                                    .build()

                                                val listener =
                                                    SplitInstallStateUpdatedListener { state ->
                                                        when (state.status()) {
                                                            SplitInstallSessionStatus.INSTALLED -> {
                                                                isDownloadingLanguage = false
                                                                showLanguageDialog = false
                                                                // Recreate activity to apply language
                                                                (context as? Activity)?.recreate()
                                                            }

                                                            SplitInstallSessionStatus.FAILED -> {
                                                                isDownloadingLanguage = false
                                                                Log.e(
                                                                    "SettingsScreen",
                                                                    "Language download failed"
                                                                )
                                                                Toast.makeText(
                                                                    context,
                                                                    "Failed to download language",
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                            }

                                                            SplitInstallSessionStatus.DOWNLOADING,
                                                            SplitInstallSessionStatus.INSTALLING -> {
                                                                // Still downloading/installing
                                                            }

                                                            else -> {
                                                                // Other states
                                                            }
                                                        }
                                                    }

                                                splitInstallManager.registerListener(listener)
                                                splitInstallManager.startInstall(request)
                                                    .addOnFailureListener {
                                                        isDownloadingLanguage = false
                                                        splitInstallManager.unregisterListener(
                                                            listener
                                                        )
                                                        // Fallback: just recreate activity
                                                        showLanguageDialog = false
                                                        (context as? Activity)?.recreate()
                                                    }
                                            } else {
                                                // System default - no download needed
                                                showLanguageDialog = false
                                                (context as? Activity)?.recreate()
                                            }
                                        }
                                    }
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedLanguage == language.code,
                                enabled = !isDownloadingLanguage,
                                onClick = null // Handled by Row's clickable
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                language.displayName,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.photo_detail_cancel))
                }
            }
        )
    }

    // Theme selection dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(stringResource(R.string.settings_theme)) },
            text = {
                Column {
                    listOf(
                        PreferencesManager.THEME_MODE_SYSTEM to R.string.settings_theme_system,
                        PreferencesManager.THEME_MODE_LIGHT to R.string.settings_theme_light,
                        PreferencesManager.THEME_MODE_DARK to R.string.settings_theme_dark
                    ).forEach { (mode, nameRes) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    coroutineScope.launch {
                                        preferencesManager.setThemeMode(mode)
                                        showThemeDialog = false
                                    }
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = themeMode == mode,
                                onClick = null // Handled by Row's clickable
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                stringResource(nameRes),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text(stringResource(R.string.photo_detail_cancel))
                }
            }
        )
    }
}

/**
 * Opens the app's page in Google Play Store for review
 */
private fun openPlayStoreForReview(context: android.content.Context) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = "market://details?id=${context.packageName}".toUri()
            setPackage("com.android.vending")
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        // Fallback to browser if Play Store is not installed
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data =
                    "https://play.google.com/store/apps/details?id=${context.packageName}".toUri()
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Log.e("SettingsScreen", "Unable to open Play Store")
        }
    }
}

