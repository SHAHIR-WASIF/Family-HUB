package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.entity.FamilyMemberEntity
import com.example.domain.DateUtils
import com.example.ui.components.EditorialEmptyState
import com.example.ui.viewmodel.GalleryPhotoItem
import com.example.ui.viewmodel.GalleryViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    onOpenMember: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: GalleryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var pendingUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var showAddMetadataDialog by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<GalleryPhotoItem?>(null) }
    var editingItem by remember { mutableStateOf<GalleryPhotoItem?>(null) }

    val multiPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        if (uris.isNotEmpty()) {
            pendingUris = uris
            showAddMetadataDialog = true
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            pendingUris = listOf(uri)
            showAddMetadataDialog = true
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
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Family Gallery") },
                actions = {
                    IconButton(
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
                        modifier = Modifier.testTag("gallery_camera_button")
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Capture family photo")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    multiPhotoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier.testTag("gallery_add_photo_fab"),
                icon = { Icon(Icons.Default.Add, contentDescription = "Add photos") },
                text = { Text("Add Photos") }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 980.dp)
                    .padding(horizontal = 16.dp)
            ) {
                // Album Filter Chips
                if (state.albums.isNotEmpty() || state.members.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = state.selectedAlbum == null && state.selectedMemberId == null,
                                onClick = {
                                    viewModel.selectAlbum(null)
                                    viewModel.selectMemberFilter(null)
                                },
                                label = { Text("All Photos") }
                            )
                        }
                        items(state.albums, key = { "alb_$it" }) { album ->
                            FilterChip(
                                selected = state.selectedAlbum == album,
                                onClick = {
                                    viewModel.selectAlbum(if (state.selectedAlbum == album) null else album)
                                },
                                label = { Text(album) }
                            )
                        }
                        items(state.members, key = { "gmem_${it.id}" }) { m ->
                            FilterChip(
                                selected = state.selectedMemberId == m.id,
                                onClick = {
                                    viewModel.selectMemberFilter(if (state.selectedMemberId == m.id) null else m.id)
                                },
                                label = { Text(m.fullName) }
                            )
                        }
                    }
                }

                if (state.photos.isEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    EditorialEmptyState(
                        drawableRes = R.drawable.img_empty_gallery,
                        title = "Preserve Family Photographs",
                        subtitle = "Create albums, tag family members, and save captions offline directly on your device.",
                        actionLabel = "Choose Photos",
                        actionTag = "gallery_empty_add_button",
                        onAction = {
                            multiPhotoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 156.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.photos, key = { it.photo.id }) { item ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(0.92f)
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable { selectedItem = item }
                                    .testTag("gallery_photo_${item.photo.id}")
                            ) {
                                AsyncImage(
                                    model = File(item.photo.filePath),
                                    contentDescription = item.photo.caption.ifBlank { item.photo.albumName },
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomCenter)
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(Color.Transparent, Color(0xBB000000))
                                            )
                                        )
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = item.photo.caption.ifBlank { item.photo.albumName },
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        item.memberName?.let { name ->
                                            Text(
                                                text = name,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White.copy(alpha = 0.8f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddMetadataDialog && pendingUris.isNotEmpty()) {
        PhotoMetadataDialog(
            title = "Save ${pendingUris.size} Photo${if (pendingUris.size > 1) "s" else ""}",
            initialAlbum = state.selectedAlbum ?: "Family Moments",
            initialCaption = "",
            initialMemberId = state.selectedMemberId,
            existingAlbums = state.albums,
            members = state.members,
            onDismiss = {
                showAddMetadataDialog = false
                pendingUris = emptyList()
            },
            onConfirm = { album, caption, memberId ->
                viewModel.addPhotos(pendingUris, album, caption, memberId, null)
                showAddMetadataDialog = false
                pendingUris = emptyList()
            }
        )
    }

    editingItem?.let { item ->
        PhotoMetadataDialog(
            title = "Edit Photo Details",
            initialAlbum = item.photo.albumName,
            initialCaption = item.photo.caption,
            initialMemberId = item.photo.memberId,
            existingAlbums = state.albums,
            members = state.members,
            onDismiss = { editingItem = null },
            onConfirm = { album, caption, memberId ->
                viewModel.updatePhotoMetadata(item.photo, album, caption, memberId)
                editingItem = null
                selectedItem = null
            }
        )
    }

    selectedItem?.let { item ->
        Dialog(onDismissRequest = { selectedItem = null }) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    AsyncImage(
                        model = File(item.photo.filePath),
                        contentDescription = item.photo.caption,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = item.photo.caption.ifBlank { item.photo.albumName },
                        style = MaterialTheme.typography.titleMedium
                    )
                    val meta = buildString {
                        append("Album: ${item.photo.albumName}")
                        item.photo.takenDateIso?.let { append(" • ${DateUtils.formatDisplayDate(it)}") }
                    }
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    item.memberName?.let { name ->
                        Spacer(Modifier.height(8.dp))
                        FilledTonalButton(
                            onClick = {
                                val mid = item.photo.memberId
                                selectedItem = null
                                if (mid != null) onOpenMember(mid)
                            }
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Tagged: $name")
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row {
                            TextButton(onClick = {
                                viewModel.deletePhoto(item.photo)
                                selectedItem = null
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Delete", color = MaterialTheme.colorScheme.error)
                            }
                            TextButton(onClick = { editingItem = item }) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Edit")
                            }
                        }
                        TextButton(onClick = { selectedItem = null }) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoMetadataDialog(
    title: String,
    initialAlbum: String,
    initialCaption: String,
    initialMemberId: Long?,
    existingAlbums: List<String>,
    members: List<FamilyMemberEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Long?) -> Unit
) {
    var album by remember { mutableStateOf(initialAlbum) }
    var caption by remember { mutableStateOf(initialCaption) }
    var memberId by remember { mutableStateOf(initialMemberId) }

    val quickAlbums = (listOf("Family Moments", "Portraits", "Reunions", "Historical") + existingAlbums).distinct()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = album,
                    onValueChange = { album = it },
                    label = { Text("Album Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(quickAlbums, key = { "qalb_$it" }) { suggestion ->
                        FilterChip(
                            selected = album == suggestion,
                            onClick = { album = suggestion },
                            label = { Text(suggestion) }
                        )
                    }
                }
                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    label = { Text("Description / Caption (Optional)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                if (members.isNotEmpty()) {
                    Text("Associate with Member (Optional)", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = memberId == null,
                                onClick = { memberId = null },
                                label = { Text("None") }
                            )
                        }
                        items(members, key = { "pmem_${it.id}" }) { m ->
                            FilterChip(
                                selected = memberId == m.id,
                                onClick = { memberId = m.id },
                                label = { Text(m.fullName) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(album, caption, memberId) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
