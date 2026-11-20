package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.FamilyMemberEntity
import com.example.data.local.entity.GalleryPhotoEntity
import com.example.data.media.MediaStorageManager
import com.example.data.repository.FamilyRepository
import com.example.domain.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class GalleryPhotoItem(
    val photo: GalleryPhotoEntity,
    val memberName: String?
)

data class GalleryUiState(
    val photos: List<GalleryPhotoItem> = emptyList(),
    val albums: List<String> = emptyList(),
    val members: List<FamilyMemberEntity> = emptyList(),
    val selectedAlbum: String? = null,
    val selectedMemberId: Long? = null
)

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val repository: FamilyRepository,
    private val mediaStorageManager: MediaStorageManager
) : ViewModel() {

    private val selectedAlbum = MutableStateFlow<String?>(null)
    private val selectedMemberId = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<GalleryUiState> = combine(
        repository.allGalleryPhotos,
        repository.allMembers,
        selectedAlbum,
        selectedMemberId
    ) { photos, members, albumFilter, memberFilter ->
        val memberMap = members.associateBy { it.id }
        val albums = photos.map { it.albumName }.filter { it.isNotBlank() }.distinct().sorted()

        val filtered = photos.filter { photo ->
            val matchAlbum = albumFilter == null || photo.albumName == albumFilter
            val matchMember = memberFilter == null || photo.memberId == memberFilter
            matchAlbum && matchMember
        }.map { photo ->
            GalleryPhotoItem(
                photo = photo,
                memberName = photo.memberId?.let { memberMap[it]?.fullName }
            )
        }

        GalleryUiState(
            photos = filtered,
            albums = albums,
            members = members,
            selectedAlbum = albumFilter,
            selectedMemberId = memberFilter
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GalleryUiState()
    )

    fun selectAlbum(album: String?) {
        selectedAlbum.value = album
    }

    fun selectMemberFilter(memberId: Long?) {
        selectedMemberId.value = memberId
    }

    fun createTempCameraUri(): Pair<Uri, File> = mediaStorageManager.createTempCameraUri()

    fun addPhotos(
        uris: List<Uri>,
        albumName: String,
        caption: String,
        memberId: Long?,
        takenDateIso: String?
    ) {
        if (uris.isEmpty()) return
        val cleanAlbum = albumName.trim().ifBlank { "Family Moments" }
        val cleanDate = takenDateIso?.takeIf { it.isNotBlank() } ?: DateUtils.toIsoDate(System.currentTimeMillis())
        viewModelScope.launch {
            uris.forEach { uri ->
                val savedPath = mediaStorageManager.saveImageFromUri(uri)
                if (savedPath != null) {
                    repository.saveGalleryPhoto(
                        GalleryPhotoEntity(
                            memberId = memberId,
                            albumName = cleanAlbum,
                            filePath = savedPath,
                            caption = caption.trim(),
                            takenDateIso = cleanDate
                        )
                    )
                }
            }
        }
    }

    fun updatePhotoMetadata(photo: GalleryPhotoEntity, albumName: String, caption: String, memberId: Long?) {
        viewModelScope.launch {
            repository.saveGalleryPhoto(
                photo.copy(
                    albumName = albumName.trim().ifBlank { "Family Moments" },
                    caption = caption.trim(),
                    memberId = memberId
                )
            )
        }
    }

    fun deletePhoto(photo: GalleryPhotoEntity) {
        viewModelScope.launch {
            repository.deleteGalleryPhoto(photo)
        }
    }
}
