package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PersonPin
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.backup.RestoreMode
import com.example.domain.DateUtils
import com.example.ui.theme.ThemeMode
import com.example.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var showClearDataDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackup(uri)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingImportUri = uri
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.setNotificationsEnabled(granted)
    }

    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings & Data") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 820.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (state.isBusy) {
                    item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
                }

                // Theme Selection
                item {
                    SettingsSectionCard(
                        icon = Icons.Default.Palette,
                        title = "Appearance & Theme"
                    ) {
                        val options = listOf(
                            ThemeMode.SYSTEM to "System",
                            ThemeMode.LIGHT to "Light",
                            ThemeMode.DARK to "Dark"
                        )
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            options.forEachIndexed { idx, (mode, label) ->
                                SegmentedButton(
                                    selected = state.preferences.themeMode == mode,
                                    onClick = { viewModel.setThemeMode(mode) },
                                    shape = SegmentedButtonDefaults.itemShape(index = idx, count = options.size),
                                    modifier = Modifier.testTag("theme_option_${mode.name.lowercase()}")
                                ) {
                                    Text(label)
                                }
                            }
                        }
                    }
                }

                // Family Owner Selection
                item {
                    SettingsSectionCard(
                        icon = Icons.Default.PersonPin,
                        title = "Family Owner (Me)"
                    ) {
                        Text(
                            text = "Select your profile to automatically display relationship labels (e.g. Your Father, Your Sister) across the app.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (state.members.isEmpty()) {
                            Text(
                                text = "Create at least one member profile first.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                item {
                                    FilterChip(
                                        selected = state.preferences.ownerMemberId == null,
                                        onClick = { viewModel.setOwnerMemberId(null) },
                                        label = { Text("None") }
                                    )
                                }
                                items(state.members, key = { "owner_${it.id}" }) { member ->
                                    FilterChip(
                                        selected = state.preferences.ownerMemberId == member.id,
                                        onClick = { viewModel.setOwnerMemberId(member.id) },
                                        label = { Text(member.fullName) }
                                    )
                                }
                            }
                        }
                    }
                }

                // JSON Backup & Restore
                item {
                    SettingsSectionCard(
                        icon = Icons.Default.Backup,
                        title = "Backup & Restore (JSON)"
                    ) {
                        Text(
                            text = "Archive contains ${state.totalMembers} profiles, ${state.totalPhotos} photos, and ${state.totalEvents} events. Backups are 100% offline JSON files with embedded media.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    val dateStamp = DateUtils.toIsoDate(System.currentTimeMillis())
                                    exportLauncher.launch("FamilyHub_Backup_$dateStamp.json")
                                },
                                modifier = Modifier.weight(1f).testTag("export_backup_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Export JSON")
                            }
                            FilledTonalButton(
                                onClick = {
                                    importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                                },
                                modifier = Modifier.weight(1f).testTag("import_backup_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Import JSON")
                            }
                        }
                    }
                }

                // Notifications
                item {
                    SettingsSectionCard(
                        icon = Icons.Default.Notifications,
                        title = "Local Reminders"
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Birthday & Event Alerts", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "Daily offline background check via WorkManager",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = state.preferences.notificationsEnabled,
                                onCheckedChange = { enable ->
                                    if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        val granted = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.POST_NOTIFICATIONS
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (granted) viewModel.setNotificationsEnabled(true)
                                        else notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        viewModel.setNotificationsEnabled(enable)
                                    }
                                },
                                modifier = Modifier.testTag("notifications_switch")
                            )
                        }

                        Text("Reminder Lead Time", style = MaterialTheme.typography.labelMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(0 to "On Day", 1 to "1 Day Before", 3 to "3 Days Before", 7 to "7 Days Before").forEach { (days, label) ->
                                FilterChip(
                                    selected = state.preferences.reminderDaysBefore == days,
                                    onClick = { viewModel.setReminderDaysBefore(days) },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }

                // Privacy Settings
                item {
                    SettingsSectionCard(
                        icon = Icons.Default.Lock,
                        title = "Privacy & Security"
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Mask Sensitive Details", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "Hide phone, email, address, blood group & private notes until tapped",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = state.preferences.hideSensitiveOverview,
                                onCheckedChange = viewModel::setHideSensitiveOverview,
                                modifier = Modifier.testTag("privacy_mask_switch")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Include Private Notes in Backups", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "Turn off to exclude Important Notes from exported JSON files",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = state.preferences.includePrivateNotesInBackup,
                                onCheckedChange = viewModel::setIncludePrivateNotesInBackup
                            )
                        }
                    }
                }

                // Danger Zone
                item {
                    SettingsSectionCard(
                        icon = Icons.Default.DeleteForever,
                        title = "Data Management"
                    ) {
                        Text(
                            text = "All data is stored locally in Room & private app storage. You can export a backup anytime before clearing.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = { showClearDataDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth().testTag("clear_all_data_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Permanently Delete All Family Data")
                        }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }

    // Import Restore Mode Dialog
    pendingImportUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            title = { Text("Restore Backup") },
            text = {
                Text("Choose how to import this JSON backup. Merging adds restored profiles alongside current data; Replacing wipes current data first.")
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.importBackup(uri, RestoreMode.MERGE_WITH_EXISTING)
                    pendingImportUri = null
                }) {
                    Text("Merge")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { pendingImportUri = null }) { Text("Cancel") }
                    TextButton(
                        onClick = {
                            viewModel.importBackup(uri, RestoreMode.REPLACE_EXISTING)
                            pendingImportUri = null
                        }
                    ) {
                        Text("Replace All", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        )
    }

    // Clear Data Confirmation Dialog
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("Erase Everything?") },
            text = { Text("This will permanently delete all family member profiles, relationships, memories, photos, and events on this device. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showClearDataDialog = false
                        viewModel.clearAllData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingsSectionCard(
    icon: ImageVector,
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text(text = title, style = MaterialTheme.typography.headlineSmall)
            }
            content()
        }
    }
}
