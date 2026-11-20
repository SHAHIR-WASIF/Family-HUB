package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.CustomFieldType
import com.example.data.local.entity.FamilyMemberEntity
import com.example.domain.DateUtils
import com.example.ui.components.FamilyDatePickerDialog
import com.example.ui.components.MemberAvatar
import com.example.ui.viewmodel.AddEditMemberViewModel

private enum class SelectorKind(val title: String, val multiSelect: Boolean) {
    FATHER("Select Father", false),
    MOTHER("Select Mother", false),
    SPOUSE("Select Spouse(s)", true),
    CHILDREN("Select Children", true),
    BROTHERS("Select Brothers", true),
    SISTERS("Select Sisters", true),
    GRANDPARENTS("Select Grandparents", true),
    OTHER_RELATIVES("Select Other Relatives", true)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditMemberScreen(
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
    onOpenExistingMember: (Long) -> Unit,
    viewModel: AddEditMemberViewModel = hiltViewModel()
) {
    val screenState by viewModel.uiState.collectAsStateWithLifecycle()
    val form = screenState.form
    val candidates = screenState.candidateMembers
    val context = LocalContext.current

    var showDobPicker by remember { mutableStateOf(false) }
    var activeSelector by remember { mutableStateOf<SelectorKind?>(null) }
    var customDateFieldKey by remember { mutableStateOf<Long?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var cameraNotice by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.setProfilePhotoFromUri(uri)
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            viewModel.setProfilePhotoFromUri(uri)
        }
        pendingCameraUri = null
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val (uri, _) = viewModel.createTempCameraUri()
            pendingCameraUri = uri
            takePictureLauncher.launch(uri)
        } else {
            cameraNotice = "Camera permission is required to capture a photo. You can still pick a photo from your gallery."
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (form.isEditMode) "Edit Family Member" else "Add Family Member") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("add_edit_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.saveMember(onSaved) },
                        enabled = form.fullName.isNotBlank() && !form.isSaving,
                        modifier = Modifier.testTag("top_save_member_button")
                    ) {
                        Text("Save")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 4.dp,
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { viewModel.saveMember(onSaved) },
                        enabled = form.fullName.isNotBlank() && !form.isSaving,
                        modifier = Modifier
                            .weight(1.4f)
                            .testTag("save_member_button"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (form.isSaving) "Saving…" else if (form.isEditMode) "Update Profile" else "Save Profile")
                    }
                }
            }
        }
    ) { innerPadding ->
        if (form.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

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
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Duplicate Warning Banner
                if (form.duplicateMatches.isNotEmpty() && !form.allowDuplicateSave) {
                    item {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Possible duplicate profile found",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "${form.duplicateMatches.first().fullName} already exists in your family list.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = { onOpenExistingMember(form.duplicateMatches.first().id) }) {
                                        Text("Open Existing")
                                    }
                                    FilledTonalButton(onClick = { viewModel.confirmDuplicateAndSave(onSaved) }) {
                                        Text("Save Anyway")
                                    }
                                }
                            }
                        }
                    }
                }

                // SECTION 1: BASIC INFORMATION
                item {
                    FormSectionCard(
                        title = "Basic Information",
                        subtitle = "Only Full Name is required — everything else is optional."
                    ) {
                        // Photo Picker + Camera Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            MemberAvatar(
                                fullName = form.fullName.ifBlank { "?" },
                                photoPath = form.profilePhotoPath,
                                memberId = form.memberId,
                                size = 76.dp
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilledTonalButton(
                                        onClick = {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        modifier = Modifier.testTag("pick_photo_button")
                                    ) {
                                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Gallery")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            val granted = ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.CAMERA
                                            ) == PackageManager.PERMISSION_GRANTED
                                            if (granted) {
                                                val (uri, _) = viewModel.createTempCameraUri()
                                                pendingCameraUri = uri
                                                takePictureLauncher.launch(uri)
                                            } else {
                                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                            }
                                        },
                                        modifier = Modifier.testTag("take_photo_button")
                                    ) {
                                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Camera")
                                    }
                                }
                                if (form.profilePhotoPath != null) {
                                    TextButton(onClick = viewModel::clearProfilePhoto) {
                                        Text("Remove Photo", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = form.fullName,
                            onValueChange = viewModel::updateFullName,
                            label = { Text("Full Name *") },
                            placeholder = { Text("Enter full name (required)") },
                            isError = form.nameError != null,
                            supportingText = form.nameError?.let { err -> { Text(err) } },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("full_name_input")
                        )

                        OutlinedTextField(
                            value = form.nickname,
                            onValueChange = viewModel::updateNickname,
                            label = { Text("Nickname (Optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("nickname_input")
                        )

                        // Date of Birth selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showDobPicker = true },
                                modifier = Modifier.weight(1f).testTag("dob_picker_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.Cake, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                val label = if (form.dateOfBirth.isBlank()) {
                                    "Date of Birth (Optional)"
                                } else {
                                    val age = DateUtils.formatAgeLabel(form.dateOfBirth)
                                    val formatted = DateUtils.formatDisplayDate(form.dateOfBirth)
                                    if (age != null) "$formatted ($age)" else formatted
                                }
                                Text(label)
                            }
                            if (form.dateOfBirth.isNotBlank()) {
                                IconButton(onClick = { viewModel.updateDateOfBirth("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear date of birth")
                                }
                            }
                        }

                        // Gender chips
                        Text("Gender (Optional)", style = MaterialTheme.typography.labelMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Female", "Male", "Other").forEach { g ->
                                FilterChip(
                                    selected = form.gender.equals(g, ignoreCase = true),
                                    onClick = {
                                        viewModel.updateGender(if (form.gender.equals(g, true)) "" else g)
                                    },
                                    label = { Text(g) }
                                )
                            }
                        }

                        // Blood group chips
                        Text("Blood Group (Optional)", style = MaterialTheme.typography.labelMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-").forEach { bg ->
                                FilterChip(
                                    selected = form.bloodGroup == bg,
                                    onClick = {
                                        viewModel.updateBloodGroup(if (form.bloodGroup == bg) "" else bg)
                                    },
                                    label = { Text(bg) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = form.biography,
                            onValueChange = viewModel::updateBiography,
                            label = { Text("Short Description / Biography (Optional)") },
                            minLines = 3,
                            maxLines = 6,
                            modifier = Modifier.fillMaxWidth().testTag("biography_input")
                        )
                    }
                }

                // SECTION 2: FAMILY RELATIONSHIPS
                item {
                    val memberLookup = candidates.associateBy { it.id }
                    FormSectionCard(
                        title = "Family Information",
                        subtitle = "Link parents, spouse, siblings, children, or extended relatives (all optional)."
                    ) {
                        OutlinedTextField(
                            value = form.relationshipToOwner,
                            onValueChange = viewModel::updateRelationshipToOwner,
                            label = { Text("Relationship to Family Owner (Optional)") },
                            placeholder = { Text("e.g., Mother, Maternal Uncle, Cousin") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Generation Level hint for unlinked trees
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Generation Tier (Fallback)", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    text = "Used when relationships aren't linked yet (${form.generationOffset})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { viewModel.updateGenerationOffset(form.generationOffset - 1) }) { Text("Older") }
                                Text("${form.generationOffset}", style = MaterialTheme.typography.titleMedium)
                                TextButton(onClick = { viewModel.updateGenerationOffset(form.generationOffset + 1) }) { Text("Younger") }
                            }
                        }

                        RelationshipSelectorRow(
                            label = "Father",
                            selectedText = form.fatherId?.let { memberLookup[it]?.fullName } ?: "Not selected",
                            onClick = { activeSelector = SelectorKind.FATHER }
                        )
                        RelationshipSelectorRow(
                            label = "Mother",
                            selectedText = form.motherId?.let { memberLookup[it]?.fullName } ?: "Not selected",
                            onClick = { activeSelector = SelectorKind.MOTHER }
                        )
                        RelationshipSelectorRow(
                            label = "Spouse(s)",
                            selectedText = formatSelectedNames(form.spouseIds, memberLookup),
                            onClick = { activeSelector = SelectorKind.SPOUSE }
                        )
                        RelationshipSelectorRow(
                            label = "Children",
                            selectedText = formatSelectedNames(form.childIds, memberLookup),
                            onClick = { activeSelector = SelectorKind.CHILDREN }
                        )
                        RelationshipSelectorRow(
                            label = "Brothers",
                            selectedText = formatSelectedNames(form.brotherIds, memberLookup),
                            onClick = { activeSelector = SelectorKind.BROTHERS }
                        )
                        RelationshipSelectorRow(
                            label = "Sisters",
                            selectedText = formatSelectedNames(form.sisterIds, memberLookup),
                            onClick = { activeSelector = SelectorKind.SISTERS }
                        )
                        RelationshipSelectorRow(
                            label = "Grandparents",
                            selectedText = formatSelectedNames(form.grandparentIds, memberLookup),
                            onClick = { activeSelector = SelectorKind.GRANDPARENTS }
                        )
                        RelationshipSelectorRow(
                            label = "Other Relatives",
                            selectedText = formatSelectedNames(form.otherRelativeIds, memberLookup),
                            onClick = { activeSelector = SelectorKind.OTHER_RELATIVES }
                        )
                    }
                }

                // SECTION 3: ADDITIONAL INFORMATION
                item {
                    FormSectionCard(
                        title = "Additional Information",
                        subtitle = "Optional life, career, and contact records."
                    ) {
                        OutlinedTextField(
                            value = form.birthplace,
                            onValueChange = viewModel::updateBirthplace,
                            label = { Text("Birthplace") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = form.currentAddress,
                            onValueChange = viewModel::updateCurrentAddress,
                            label = { Text("Current Address") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = form.nationality,
                            onValueChange = viewModel::updateNationality,
                            label = { Text("Nationality") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = form.occupation,
                            onValueChange = viewModel::updateOccupation,
                            label = { Text("Occupation") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = form.education,
                            onValueChange = viewModel::updateEducation,
                            label = { Text("Education") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = form.phoneNumber,
                            onValueChange = viewModel::updatePhoneNumber,
                            label = { Text("Phone Number") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = form.emailAddress,
                            onValueChange = viewModel::updateEmailAddress,
                            label = { Text("Email Address") },
                            isError = form.emailError != null,
                            supportingText = form.emailError?.let { err -> { Text(err) } },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = form.personalWebsite,
                            onValueChange = viewModel::updatePersonalWebsite,
                            label = { Text("Personal Website") },
                            isError = form.websiteError != null,
                            supportingText = form.websiteError?.let { err -> { Text(err) } },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = form.importantNotes,
                            onValueChange = viewModel::updateImportantNotes,
                            label = { Text("Important Notes") },
                            minLines = 2,
                            maxLines = 5,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // SECTION 4: CUSTOM FIELDS
                item {
                    FormSectionCard(
                        title = "Custom Fields",
                        subtitle = "Add any custom attributes (Favorite Food, Hobbies, Special Talents…)."
                    ) {
                        form.customFields.forEach { cf ->
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        OutlinedTextField(
                                            value = cf.fieldName,
                                            onValueChange = { viewModel.updateCustomField(cf.localKey, it, cf.fieldType, cf.fieldValue) },
                                            label = { Text("Field Name") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(onClick = { viewModel.removeCustomField(cf.localKey) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remove custom field")
                                        }
                                    }

                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        CustomFieldType.entries.forEach { t ->
                                            AssistChip(
                                                onClick = { viewModel.updateCustomField(cf.localKey, cf.fieldName, t, cf.fieldValue) },
                                                label = { Text(t.displayName) },
                                                colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
                                                    containerColor = if (cf.fieldType == t) MaterialTheme.colorScheme.primaryContainer
                                                    else MaterialTheme.colorScheme.surface
                                                )
                                            )
                                        }
                                    }

                                    when (cf.fieldType) {
                                        CustomFieldType.BOOLEAN -> {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Value (Optional)")
                                                Switch(
                                                    checked = cf.fieldValue.equals("Yes", true),
                                                    onCheckedChange = { checked ->
                                                        viewModel.updateCustomField(cf.localKey, cf.fieldName, cf.fieldType, if (checked) "Yes" else "No")
                                                    }
                                                )
                                            }
                                        }
                                        CustomFieldType.DATE -> {
                                            OutlinedButton(
                                                onClick = { customDateFieldKey = cf.localKey },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    if (cf.fieldValue.isBlank()) "Select Date (Optional)"
                                                    else DateUtils.formatDisplayDate(cf.fieldValue).ifEmpty { cf.fieldValue }
                                                )
                                            }
                                        }
                                        else -> {
                                            OutlinedTextField(
                                                value = cf.fieldValue,
                                                onValueChange = { viewModel.updateCustomField(cf.localKey, cf.fieldName, cf.fieldType, it) },
                                                label = { Text("Value (Optional — may leave empty)") },
                                                keyboardOptions = KeyboardOptions(
                                                    keyboardType = if (cf.fieldType == CustomFieldType.NUMBER) KeyboardType.Number else KeyboardType.Text
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        FilledTonalButton(
                            onClick = viewModel::addCustomField,
                            modifier = Modifier.fillMaxWidth().testTag("form_add_custom_field_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Add Custom Field")
                        }
                    }
                }

                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }

    if (showDobPicker) {
        FamilyDatePickerDialog(
            initialIsoDate = form.dateOfBirth.takeIf { it.isNotBlank() },
            onDateSelected = { viewModel.updateDateOfBirth(it) },
            onDismiss = { showDobPicker = false }
        )
    }

    customDateFieldKey?.let { key ->
        val target = form.customFields.firstOrNull { it.localKey == key }
        FamilyDatePickerDialog(
            initialIsoDate = target?.fieldValue?.takeIf { it.isNotBlank() },
            onDateSelected = { dateStr ->
                if (target != null) {
                    viewModel.updateCustomField(key, target.fieldName, target.fieldType, dateStr)
                }
            },
            onDismiss = { customDateFieldKey = null }
        )
    }

    cameraNotice?.let { notice ->
        AlertDialog(
            onDismissRequest = { cameraNotice = null },
            title = { Text("Camera Permission") },
            text = { Text(notice) },
            confirmButton = {
                TextButton(onClick = { cameraNotice = null }) { Text("OK") }
            }
        )
    }

    activeSelector?.let { kind ->
        val currentSelection: Set<Long> = when (kind) {
            SelectorKind.FATHER -> setOfNotNull(form.fatherId)
            SelectorKind.MOTHER -> setOfNotNull(form.motherId)
            SelectorKind.SPOUSE -> form.spouseIds.toSet()
            SelectorKind.CHILDREN -> form.childIds.toSet()
            SelectorKind.BROTHERS -> form.brotherIds.toSet()
            SelectorKind.SISTERS -> form.sisterIds.toSet()
            SelectorKind.GRANDPARENTS -> form.grandparentIds.toSet()
            SelectorKind.OTHER_RELATIVES -> form.otherRelativeIds.toSet()
        }
        SearchableMemberPickerDialog(
            title = kind.title,
            multiSelect = kind.multiSelect,
            candidates = candidates,
            initialSelection = currentSelection,
            onDismiss = { activeSelector = null },
            onConfirm = { picked ->
                when (kind) {
                    SelectorKind.FATHER -> viewModel.setFather(picked.firstOrNull())
                    SelectorKind.MOTHER -> viewModel.setMother(picked.firstOrNull())
                    SelectorKind.SPOUSE -> viewModel.setSpouses(picked.toList())
                    SelectorKind.CHILDREN -> viewModel.setChildren(picked.toList())
                    SelectorKind.BROTHERS -> viewModel.setBrothers(picked.toList())
                    SelectorKind.SISTERS -> viewModel.setSisters(picked.toList())
                    SelectorKind.GRANDPARENTS -> viewModel.setGrandparents(picked.toList())
                    SelectorKind.OTHER_RELATIVES -> viewModel.setOtherRelatives(picked.toList())
                }
                activeSelector = null
            }
        )
    }
}

private fun formatSelectedNames(ids: List<Long>, map: Map<Long, FamilyMemberEntity>): String {
    val names = ids.mapNotNull { map[it]?.fullName }
    return if (names.isEmpty()) "None selected" else names.joinToString(", ")
}

@Composable
private fun FormSectionCard(
    title: String,
    subtitle: String,
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
            Column {
                Text(text = title, style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            content()
        }
    }
}

@Composable
private fun RelationshipSelectorRow(
    label: String,
    selectedText: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(
                    text = selectedText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text("Choose", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun SearchableMemberPickerDialog(
    title: String,
    multiSelect: Boolean,
    candidates: List<FamilyMemberEntity>,
    initialSelection: Set<Long>,
    onDismiss: () -> Unit,
    onConfirm: (Set<Long>) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(initialSelection) }

    val filtered = remember(query, candidates) {
        if (query.isBlank()) candidates
        else candidates.filter {
            it.fullName.contains(query.trim(), ignoreCase = true) ||
                (it.nickname?.contains(query.trim(), ignoreCase = true) == true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (candidates.isEmpty()) {
                    Text(
                        text = "No other family members exist yet. Save this profile first, then link relatives anytime.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search member…") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (!multiSelect) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selected = emptySet() }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selected.isEmpty(),
                                        onClick = { selected = emptySet() }
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("None / Clear selection")
                                }
                            }
                        }
                        items(filtered, key = { it.id }) { member ->
                            val isSelected = member.id in selected
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selected = if (multiSelect) {
                                            if (isSelected) selected - member.id else selected + member.id
                                        } else {
                                            setOf(member.id)
                                        }
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (multiSelect) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            selected = if (checked) selected + member.id else selected - member.id
                                        }
                                    )
                                } else {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selected = setOf(member.id) }
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                MemberAvatar(
                                    fullName = member.fullName,
                                    photoPath = member.profilePhotoPath,
                                    memberId = member.id,
                                    size = 36.dp
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(member.fullName, style = MaterialTheme.typography.bodyMedium)
                                    member.dateOfBirth?.let {
                                        Text(
                                            DateUtils.extractBirthYear(it)?.let { y -> "b. $y" } ?: "",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selected) }) { Text("Done") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
