package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.local.entity.CustomFieldEntity
import com.example.data.local.entity.CustomFieldType
import com.example.data.local.entity.GalleryPhotoEntity
import com.example.data.local.entity.MemoryCategory
import com.example.data.local.entity.MemoryEntity
import com.example.domain.DateUtils
import com.example.ui.components.FamilyDatePickerDialog
import com.example.ui.components.MemberAvatar
import com.example.ui.components.shareExportedFile
import com.example.ui.viewmodel.MemberDetailViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MemberDetailScreen(
    onBack: () -> Unit,
    onEditMember: (Long) -> Unit,
    onOpenRelative: (Long) -> Unit,
    viewModel: MemberDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showShareMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteAssociatedMedia by remember { mutableStateOf(true) }

    var editingCustomField by remember { mutableStateOf<CustomFieldEntity?>(null) }
    var showAddCustomField by remember { mutableStateOf(false) }
    var showAddMemoryDialog by remember { mutableStateOf(false) }
    var viewingPhoto by remember { mutableStateOf<GalleryPhotoEntity?>(null) }

    val memberPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.addMemberGalleryPhoto(uri, caption = "", albumName = "Personal Photos")
        }
    }

    LaunchedEffect(state.exportMessage) {
        state.exportMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.profile?.member?.fullName ?: "Member Profile",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("detail_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val member = state.profile?.member
                    if (member != null) {
                        IconButton(onClick = viewModel::togglePinned) {
                            Icon(
                                imageVector = if (member.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                                contentDescription = if (member.isPinned) "Unpin member" else "Pin member",
                                tint = if (member.isPinned) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = viewModel::toggleFavorite) {
                            Icon(
                                imageVector = if (member.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (member.isFavorite) "Remove favorite" else "Mark favorite",
                                tint = if (member.isFavorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Box {
                            IconButton(onClick = { showShareMenu = true }, modifier = Modifier.testTag("detail_share_button")) {
                                Icon(Icons.Default.Share, contentDescription = "Share profile")
                            }
                            DropdownMenu(expanded = showShareMenu, onDismissRequest = { showShareMenu = false }) {
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) },
                                    text = { Text("Share as PDF") },
                                    onClick = {
                                        showShareMenu = false
                                        viewModel.exportProfilePdf { uri, mime ->
                                            shareExportedFile(context, uri, mime, "Share ${member.fullName}'s Profile PDF")
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Default.Image, contentDescription = null) },
                                    text = { Text("Share as Summary Image") },
                                    onClick = {
                                        showShareMenu = false
                                        viewModel.exportProfileImage { uri, mime ->
                                            shareExportedFile(context, uri, mime, "Share ${member.fullName}'s Profile Card")
                                        }
                                    }
                                )
                            }
                        }
                        IconButton(onClick = { onEditMember(member.id) }, modifier = Modifier.testTag("detail_edit_button")) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit profile")
                        }
                        Box {
                            IconButton(onClick = { showOverflowMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More options")
                            }
                            DropdownMenu(expanded = showOverflowMenu, onDismissRequest = { showOverflowMenu = false }) {
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                                    text = { Text("Set as Family Owner (Me)") },
                                    onClick = {
                                        showOverflowMenu = false
                                        viewModel.setAsFamilyOwner()
                                    }
                                )
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    text = { Text("Delete Member", color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showOverflowMenu = false
                                        showDeleteDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val profile = state.profile
        if (profile == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Profile not found", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = onBack) { Text("Go Back") }
                }
            }
            return@Scaffold
        }

        val m = profile.member

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
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Hero Profile Card
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
                        tonalElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val hasPhoto = !m.profilePhotoPath.isNullOrBlank() && File(m.profilePhotoPath).exists()
                            if (hasPhoto) {
                                AsyncImage(
                                    model = File(m.profilePhotoPath),
                                    contentDescription = "Profile photo of ${m.fullName}",
                                    modifier = Modifier
                                        .size(136.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                MemberAvatar(
                                    fullName = m.fullName,
                                    photoPath = null,
                                    memberId = m.id,
                                    size = 120.dp
                                )
                            }

                            Spacer(Modifier.height(14.dp))
                            Text(
                                text = m.fullName,
                                style = MaterialTheme.typography.displaySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            m.nickname?.takeIf { it.isNotBlank() }?.let { nick ->
                                Text(
                                    text = "\"$nick\"",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }

                            Spacer(Modifier.height(10.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                state.ownerLabel?.let { label ->
                                    BadgePill(text = label, highlighted = true)
                                }
                                m.dateOfBirth?.let { dob ->
                                    val age = DateUtils.formatAgeLabel(dob)
                                    val text = if (age != null) "${DateUtils.formatDisplayDate(dob)} • $age"
                                    else DateUtils.formatDisplayDate(dob)
                                    BadgePill(text = text, icon = Icons.Default.Cake)
                                }
                                m.gender?.takeIf { it.isNotBlank() }?.let {
                                    BadgePill(text = it)
                                }
                                m.bloodGroup?.takeIf { it.isNotBlank() }?.let { bg ->
                                    val displayBg = if (state.hideSensitiveDetails) "Blood: •••" else "Blood: $bg"
                                    BadgePill(text = displayBg)
                                }
                            }

                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "Created ${DateUtils.formatTimestamp(m.createdAt)} • Updated ${DateUtils.formatTimestamp(m.updatedAt)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Tabs
                item {
                    val tabs = listOf(
                        "Overview",
                        "Family (${state.relationshipGroups.sumOf { it.members.size }})",
                        "Memories (${profile.memories.size})",
                        "Photos (${profile.photos.size})"
                    )
                    TabRow(selectedTabIndex = selectedTab) {
                        tabs.forEachIndexed { idx, title ->
                            Tab(
                                selected = selectedTab == idx,
                                onClick = { selectedTab = idx },
                                text = {
                                    Text(
                                        text = title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            )
                        }
                    }
                }

                when (selectedTab) {
                    // TAB 0: OVERVIEW
                    0 -> {
                        if (state.hideSensitiveDetails) {
                            item {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.tertiaryContainer
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Privacy mask enabled for contact & medical fields",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            modifier = Modifier.weight(1f)
                                        )
                                        TextButton(onClick = viewModel::toggleRevealSensitive) {
                                            Icon(
                                                imageVector = if (state.isSensitiveRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text("Reveal")
                                        }
                                    }
                                }
                            }
                        }

                        if (!m.biography.isNullOrBlank()) {
                            item {
                                DetailSectionCard(title = "Biography") {
                                    Text(
                                        text = m.biography,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }

                        val infoPairs = buildList {
                            m.birthplace?.takeIf { it.isNotBlank() }?.let { add("Birthplace" to it) }
                            m.currentAddress?.takeIf { it.isNotBlank() }?.let {
                                add("Current Address" to if (state.hideSensitiveDetails) "••••••••••" else it)
                            }
                            m.nationality?.takeIf { it.isNotBlank() }?.let { add("Nationality" to it) }
                            m.occupation?.takeIf { it.isNotBlank() }?.let { add("Occupation" to it) }
                            m.education?.takeIf { it.isNotBlank() }?.let { add("Education" to it) }
                            m.phoneNumber?.takeIf { it.isNotBlank() }?.let {
                                add("Phone" to if (state.hideSensitiveDetails) "••••••••••" else it)
                            }
                            m.emailAddress?.takeIf { it.isNotBlank() }?.let {
                                add("Email" to if (state.hideSensitiveDetails) "••••••••••" else it)
                            }
                            m.personalWebsite?.takeIf { it.isNotBlank() }?.let { add("Website" to it) }
                            m.importantNotes?.takeIf { it.isNotBlank() }?.let {
                                add("Important Notes" to if (state.hideSensitiveDetails) "Hidden by privacy setting" else it)
                            }
                        }

                        if (infoPairs.isNotEmpty()) {
                            item {
                                DetailSectionCard(title = "Personal & Contact Details") {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        infoPairs.forEach { (label, value) ->
                                            Column {
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = value,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Custom Fields Section
                        item {
                            DetailSectionCard(
                                title = "Custom Fields",
                                trailingAction = {
                                    TextButton(onClick = { showAddCustomField = true }) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Add Field")
                                    }
                                }
                            ) {
                                if (profile.customFields.isEmpty()) {
                                    Text(
                                        text = "Add custom details like Favorite Food, Hobbies, or Special Talents.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        profile.customFields.forEach { field ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "${field.fieldName} • ${field.fieldType.displayName}",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    val displayVal = when {
                                                        field.fieldValue.isBlank() -> "Empty"
                                                        field.fieldType == CustomFieldType.DATE -> DateUtils.formatDisplayDate(field.fieldValue).ifEmpty { field.fieldValue }
                                                        else -> field.fieldValue
                                                    }
                                                    Text(
                                                        text = displayVal,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = if (field.fieldValue.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                                                        else MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                                IconButton(onClick = { editingCustomField = field }) {
                                                    Icon(Icons.Default.Edit, contentDescription = "Edit ${field.fieldName}", modifier = Modifier.size(18.dp))
                                                }
                                                IconButton(onClick = { viewModel.deleteCustomField(field.id) }) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete ${field.fieldName}", modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // TAB 1: FAMILY RELATIONSHIPS
                    1 -> {
                        if (state.relationshipGroups.isEmpty()) {
                            item {
                                DetailSectionCard(title = "Family Connections") {
                                    Text(
                                        text = "No family relationships linked yet. Tap Edit to connect parents, spouse, siblings, or children.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    FilledTonalButton(onClick = { onEditMember(m.id) }) {
                                        Text("Link Family Members")
                                    }
                                }
                            }
                        } else {
                            items(state.relationshipGroups, key = { it.type.name }) { group ->
                                DetailSectionCard(title = group.label) {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        group.members.forEach { relMember ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .clickable { onOpenRelative(relMember.id) }
                                                    .padding(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                MemberAvatar(
                                                    fullName = relMember.fullName,
                                                    photoPath = relMember.profilePhotoPath,
                                                    memberId = relMember.id,
                                                    size = 46.dp
                                                )
                                                Spacer(Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(relMember.fullName, style = MaterialTheme.typography.titleMedium)
                                                    val sub = DateUtils.formatAgeLabel(relMember.dateOfBirth)
                                                        ?: relMember.nickname
                                                        ?: "View Profile"
                                                    Text(
                                                        text = sub,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // TAB 2: MEMORIES & HISTORICAL DOCUMENTS
                    2 -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = { showAddMemoryDialog = true },
                                    modifier = Modifier.fillMaxWidth().testTag("add_memory_button"),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Add Life Event, Story or Document")
                                }
                            }
                        }
                        if (profile.memories.isEmpty()) {
                            item {
                                DetailSectionCard(title = "Stories & Milestones") {
                                    Text(
                                        text = "Record life events, achievements, family contributions, stories, or attach historical family documents.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            items(profile.memories, key = { it.id }) { mem ->
                                ElevatedCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    shape = MaterialTheme.shapes.medium,
                                    colors = CardDefaults.elevatedCardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(50.dp),
                                                color = MaterialTheme.colorScheme.tertiaryContainer
                                            ) {
                                                Text(
                                                    text = mem.category.displayName,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                )
                                            }
                                            IconButton(onClick = { viewModel.deleteMemory(mem) }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete memory", modifier = Modifier.size(18.dp))
                                            }
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        Text(text = mem.title, style = MaterialTheme.typography.titleMedium)
                                        mem.dateIso?.let {
                                            Text(
                                                text = DateUtils.formatDisplayDate(it),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        if (mem.description.isNotBlank()) {
                                            Spacer(Modifier.height(6.dp))
                                            Text(text = mem.description, style = MaterialTheme.typography.bodyMedium)
                                        }
                                        mem.attachmentPath?.takeIf { File(it).exists() }?.let { att ->
                                            Spacer(Modifier.height(10.dp))
                                            if (att.endsWith(".jpg", true) || att.endsWith(".png", true)) {
                                                AsyncImage(
                                                    model = File(att),
                                                    contentDescription = "Memory attachment",
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(170.dp)
                                                        .clip(RoundedCornerShape(14.dp)),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(10.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                                        Spacer(Modifier.width(6.dp))
                                                        Text("Attached Document: ${File(att).name}", style = MaterialTheme.typography.labelMedium)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // TAB 3: PERSONAL PHOTO GALLERY
                    3 -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = {
                                        memberPhotoPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("add_member_photo_button"),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Add Photo to ${m.fullName}'s Gallery")
                                }
                            }
                        }
                        if (profile.photos.isEmpty()) {
                            item {
                                DetailSectionCard(title = "Personal Gallery") {
                                    Text(
                                        text = "No personal photos added yet. Tap above to add portraits or family snapshots.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            items(profile.photos.chunked(2)) { rowPhotos ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    rowPhotos.forEach { photo ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(16.dp))
                                                .clickable { viewingPhoto = photo }
                                        ) {
                                            AsyncImage(
                                                model = File(photo.filePath),
                                                contentDescription = photo.caption.ifBlank { "Family photo" },
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    }
                                    if (rowPhotos.size == 1) {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Profile?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Are you sure you want to permanently remove ${state.profile?.member?.fullName}? This will also remove their relationship links, custom fields, and memories.")
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { deleteAssociatedMedia = !deleteAssociatedMedia }
                    ) {
                        Checkbox(
                            checked = deleteAssociatedMedia,
                            onCheckedChange = { deleteAssociatedMedia = it }
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Permanently delete associated local photos & documents",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteMember(deleteAssociatedMedia, onBack)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Add / Edit Custom Field Dialog
    if (showAddCustomField || editingCustomField != null) {
        CustomFieldEditorDialog(
            initial = editingCustomField,
            onDismiss = {
                showAddCustomField = false
                editingCustomField = null
            },
            onSave = { id, name, type, value ->
                viewModel.saveCustomField(id, name, type, value)
                showAddCustomField = false
                editingCustomField = null
            }
        )
    }

    // Add Memory Dialog
    if (showAddMemoryDialog) {
        AddMemoryDialog(
            onDismiss = { showAddMemoryDialog = false },
            onSave = { category, title, desc, dateIso, uri ->
                viewModel.saveMemory(0L, category, title, desc, dateIso, uri, null)
                showAddMemoryDialog = false
            }
        )
    }

    // Fullscreen Photo Viewer
    viewingPhoto?.let { photo ->
        Dialog(onDismissRequest = { viewingPhoto = null }) {
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
                Column(modifier = Modifier.padding(16.dp)) {
                    AsyncImage(
                        model = File(photo.filePath),
                        contentDescription = photo.caption,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Fit
                    )
                    if (photo.caption.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(photo.caption, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            onClick = {
                                viewModel.deleteGalleryPhoto(photo)
                                viewingPhoto = null
                            }
                        ) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                        TextButton(onClick = { viewingPhoto = null }) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BadgePill(
    text: String,
    highlighted: Boolean = false,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Surface(
        shape = RoundedCornerShape(50.dp),
        color = if (highlighted) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = if (highlighted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(5.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = if (highlighted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun DetailSectionCard(
    title: String,
    trailingAction: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                trailingAction?.invoke()
            }
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomFieldEditorDialog(
    initial: CustomFieldEntity?,
    onDismiss: () -> Unit,
    onSave: (Long, String, CustomFieldType, String) -> Unit
) {
    var name by remember { mutableStateOf(initial?.fieldName ?: "") }
    var type by remember { mutableStateOf(initial?.fieldType ?: CustomFieldType.TEXT) }
    var value by remember { mutableStateOf(initial?.fieldValue ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add Custom Field" else "Edit Custom Field") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Field Name *") },
                    placeholder = { Text("e.g., Favorite Food, Hobbies") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Field Type", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CustomFieldType.entries.forEach { t ->
                        AssistChip(
                            onClick = { type = t },
                            label = { Text(t.displayName) },
                            colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
                                containerColor = if (type == t) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                }
                when (type) {
                    CustomFieldType.BOOLEAN -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Value (Optional)")
                            Switch(
                                checked = value.equals("Yes", true) || value.equals("true", true),
                                onCheckedChange = { value = if (it) "Yes" else "No" }
                            )
                        }
                    }
                    CustomFieldType.DATE -> {
                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (value.isBlank()) "Pick Date (Optional)" else DateUtils.formatDisplayDate(value).ifEmpty { value })
                        }
                    }
                    else -> {
                        OutlinedTextField(
                            value = value,
                            onValueChange = { value = it },
                            label = { Text("Value (Optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(initial?.id ?: 0L, name, type, value) },
                enabled = name.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showDatePicker) {
        FamilyDatePickerDialog(
            initialIsoDate = value.takeIf { it.isNotBlank() },
            onDateSelected = { value = it },
            onDismiss = { showDatePicker = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddMemoryDialog(
    onDismiss: () -> Unit,
    onSave: (MemoryCategory, String, String, String?, Uri?) -> Unit
) {
    var category by remember { mutableStateOf(MemoryCategory.LIFE_EVENT) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dateIso by remember { mutableStateOf("") }
    var attachmentUri by remember { mutableStateOf<Uri?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val docPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> attachmentUri = uri }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Memory or Document") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MemoryCategory.entries.forEach { cat ->
                        AssistChip(
                            onClick = { category = cat },
                            label = { Text(cat.displayName, style = MaterialTheme.typography.labelSmall) },
                            colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
                                containerColor = if (category == cat) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Story / Details (Optional)") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.weight(1f)) {
                        Text(if (dateIso.isBlank()) "Date" else DateUtils.formatDisplayDate(dateIso))
                    }
                    OutlinedButton(
                        onClick = { docPicker.launch(arrayOf("image/*", "application/pdf")) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (attachmentUri == null) "Attach" else "Attached ✓")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(category, title, description, dateIso.takeIf { it.isNotBlank() }, attachmentUri) },
                enabled = title.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showDatePicker) {
        FamilyDatePickerDialog(
            initialIsoDate = dateIso.takeIf { it.isNotBlank() },
            onDateSelected = { dateIso = it },
            onDismiss = { showDatePicker = false }
        )
    }
}
