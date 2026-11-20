package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.FamilyEventType
import com.example.data.local.entity.FamilyMemberEntity
import com.example.domain.DateUtils
import com.example.ui.components.FamilyDatePickerDialog
import com.example.ui.components.MemberAvatar
import com.example.ui.components.SectionHeader
import com.example.ui.viewmodel.EventsViewModel
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(
    onOpenMember: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: EventsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.enableNotifications()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Birthdays & Events") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("add_event_fab"),
                icon = { Icon(Icons.Default.Add, contentDescription = "Add event") },
                text = { Text("Add Event") }
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
                    .widthIn(max = 880.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Notification Opt-in Banner
                if (!state.notificationsEnabled) {
                    item {
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Local Birthday Reminders",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "Get offline notifications before birthdays & anniversaries.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            val granted = ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.POST_NOTIFICATIONS
                                            ) == PackageManager.PERMISSION_GRANTED
                                            if (granted) viewModel.enableNotifications()
                                            else notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            viewModel.enableNotifications()
                                        }
                                    }
                                ) {
                                    Text("Enable")
                                }
                            }
                        }
                    }
                }

                // Birthday Calendar Month Selector
                item {
                    SectionHeader(title = "Birthday Calendar")
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items((1..12).toList(), key = { "month_$it" }) { m ->
                            val monthName = Month.of(m).getDisplayName(TextStyle.SHORT, Locale.getDefault())
                            val bdayCount = state.birthdays.count {
                                DateUtils.parseIsoDate(it.member.dateOfBirth)?.monthValue == m
                            }
                            FilterChip(
                                selected = state.selectedMonth == m,
                                onClick = { viewModel.selectMonth(m) },
                                label = {
                                    Text(if (bdayCount > 0) "$monthName ($bdayCount)" else monthName)
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    val monthBirthdays = state.birthdays.filter {
                        DateUtils.parseIsoDate(it.member.dateOfBirth)?.monthValue == state.selectedMonth
                    }.sortedBy { DateUtils.parseIsoDate(it.member.dateOfBirth)?.dayOfMonth ?: 0 }

                    ElevatedCard(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val selectedMonthTitle = Month.of(state.selectedMonth)
                                .getDisplayName(TextStyle.FULL, Locale.getDefault())
                            Text(
                                text = "$selectedMonthTitle Birthdays",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (monthBirthdays.isEmpty()) {
                                Text(
                                    text = "No family birthdays recorded in $selectedMonthTitle.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                monthBirthdays.forEach { b ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { onOpenMember(b.member.id) }
                                            .padding(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        MemberAvatar(
                                            fullName = b.member.fullName,
                                            photoPath = b.member.profilePhotoPath,
                                            memberId = b.member.id,
                                            size = 40.dp
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(b.member.fullName, style = MaterialTheme.typography.titleSmall)
                                            val turning = b.turningAge?.let { " • Turning $it" } ?: ""
                                            Text(
                                                text = "${b.displayDate}$turning",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = when (b.daysUntil) {
                                                0L -> "Today!"
                                                1L -> "Tomorrow"
                                                else -> "In ${b.daysUntil}d"
                                            },
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.secondary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // All Upcoming Birthdays
                item {
                    SectionHeader(title = "Upcoming Birthdays")
                }
                if (state.birthdays.isEmpty()) {
                    item {
                        Text(
                            text = "Add dates of birth on family member profiles to track birthdays automatically.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(state.birthdays.take(10), key = { "up_bday_${it.member.id}" }) { bday ->
                        ElevatedCard(
                            onClick = { onOpenMember(bday.member.id) },
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MemberAvatar(
                                    fullName = bday.member.fullName,
                                    photoPath = bday.member.profilePhotoPath,
                                    memberId = bday.member.id,
                                    size = 48.dp
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(bday.member.fullName, style = MaterialTheme.typography.titleMedium)
                                    val ageLabel = bday.turningAge?.let { "Turns $it on ${bday.displayDate}" } ?: bday.displayDate
                                    Text(
                                        text = ageLabel,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(50.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Cake, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = when (bday.daysUntil) {
                                                0L -> "Today!"
                                                1L -> "1 day"
                                                else -> "${bday.daysUntil}d"
                                            },
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Anniversaries & Custom Family Events
                item {
                    SectionHeader(title = "Anniversaries & Family Events")
                }
                if (state.events.isEmpty()) {
                    item {
                        ElevatedCard(
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Celebration,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text("No custom events yet", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "Tap Add Event to record anniversaries, reunions, and milestones.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(state.events, key = { "ev_${it.event.id}" }) { row ->
                        ElevatedCard(
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.tertiaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Celebration,
                                        contentDescription = row.event.title,
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(row.event.title, style = MaterialTheme.typography.titleMedium)
                                    val sub = buildString {
                                        append(row.event.eventType.displayName)
                                        append(" • ")
                                        append(row.displayDate)
                                        row.relatedMemberName?.let { append(" • $it") }
                                    }
                                    Text(
                                        text = sub,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (row.event.description.isNotBlank()) {
                                        Text(
                                            text = row.event.description,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                                row.daysUntil?.let { d ->
                                    Surface(
                                        shape = RoundedCornerShape(50.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = if (d == 0L) "Today" else "${d}d",
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                IconButton(onClick = { viewModel.deleteEvent(row.event.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete event", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddFamilyEventDialog(
            members = state.members,
            onDismiss = { showAddDialog = false },
            onSave = { title, desc, dateIso, type, memberId, recurring, reminder ->
                viewModel.addEvent(title, desc, dateIso, type, memberId, recurring, reminder)
                showAddDialog = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddFamilyEventDialog(
    members: List<FamilyMemberEntity>,
    onDismiss: () -> Unit,
    onSave: (String, String, String, FamilyEventType, Long?, Boolean, Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dateIso by remember { mutableStateOf(DateUtils.toIsoDate(System.currentTimeMillis())) }
    var type by remember { mutableStateOf(FamilyEventType.ANNIVERSARY) }
    var selectedMemberId by remember { mutableStateOf<Long?>(null) }
    var recurring by remember { mutableStateOf(true) }
    var reminder by remember { mutableStateOf(true) }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Family Event") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Event Title *") },
                    placeholder = { Text("e.g., Parents' Wedding Anniversary") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("event_title_input")
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FamilyEventType.entries.forEach { t ->
                        AssistChip(
                            onClick = { type = t },
                            label = { Text(t.displayName, style = MaterialTheme.typography.labelSmall) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (type == t) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                }
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Date: ${DateUtils.formatDisplayDate(dateIso)}")
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Notes (Optional)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                if (members.isNotEmpty()) {
                    Text("Related Member (Optional)", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = selectedMemberId == null,
                                onClick = { selectedMemberId = null },
                                label = { Text("None") }
                            )
                        }
                        items(members, key = { "ev_mem_${it.id}" }) { m ->
                            FilterChip(
                                selected = selectedMemberId == m.id,
                                onClick = { selectedMemberId = m.id },
                                label = { Text(m.fullName) }
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Repeats yearly", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = recurring, onCheckedChange = { recurring = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable reminder", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = reminder, onCheckedChange = { reminder = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, description, dateIso, type, selectedMemberId, recurring, reminder) },
                enabled = title.isNotBlank() && dateIso.isNotBlank(),
                modifier = Modifier.testTag("save_event_button")
            ) {
                Text("Save Event")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showDatePicker) {
        FamilyDatePickerDialog(
            initialIsoDate = dateIso,
            onDateSelected = { dateIso = it },
            onDismiss = { showDatePicker = false }
        )
    }
}
