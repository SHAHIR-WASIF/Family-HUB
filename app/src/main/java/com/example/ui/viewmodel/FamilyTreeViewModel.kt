package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.media.MediaStorageManager
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.FamilyRepository
import com.example.domain.DateUtils
import com.example.domain.FamilyTreeLayoutEngine
import com.example.domain.TreeEdgeKind
import com.example.domain.TreeLayoutResult
import com.example.util.PdfExportHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class FamilyTreeUiState(
    val layout: TreeLayoutResult = TreeLayoutResult(emptyList(), emptyList(), emptyList(), 600f, 400f),
    val focusMemberId: Long? = null,
    val selectedGenerationFilter: Int? = null,
    val message: String? = null
)

@HiltViewModel
class FamilyTreeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    repository: FamilyRepository,
    private val mediaStorageManager: MediaStorageManager,
    preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val focusMemberId = MutableStateFlow<Long?>(null)
    private val selectedGeneration = MutableStateFlow<Int?>(null)
    private val message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<FamilyTreeUiState> = combine(
        repository.allMembers,
        repository.allRelationships,
        preferencesRepository.preferences,
        focusMemberId,
        selectedGeneration,
        message
    ) { arr ->
        @Suppress("UNCHECKED_CAST")
        val members = arr[0] as List<com.example.data.local.entity.FamilyMemberEntity>
        @Suppress("UNCHECKED_CAST")
        val rels = arr[1] as List<com.example.data.local.entity.RelationshipEntity>
        val prefs = arr[2] as com.example.data.preferences.UserPreferences
        val focus = arr[3] as Long? ?: prefs.ownerMemberId
        val genFilter = arr[4] as Int?
        val msg = arr[5] as String?

        val layout = FamilyTreeLayoutEngine.buildLayout(members, rels, focus)
        FamilyTreeUiState(
            layout = layout,
            focusMemberId = focus,
            selectedGenerationFilter = genFilter,
            message = msg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FamilyTreeUiState()
    )

    fun setFocusMember(memberId: Long?) {
        focusMemberId.value = memberId
    }

    fun selectGeneration(gen: Int?) {
        selectedGeneration.value = gen
    }

    fun exportTreePdf(onReady: (Uri) -> Unit) {
        val layout = uiState.value.layout
        if (layout.nodes.isEmpty()) {
            message.value = "Add at least one family member before exporting."
            return
        }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val pdfNodes = layout.nodes.map { node ->
                        val birthYear = DateUtils.extractBirthYear(node.member.dateOfBirth)
                        val sub = buildString {
                            if (birthYear != null) append("b. $birthYear")
                            if (!node.member.relationshipToOwner.isNullOrBlank()) {
                                if (isNotEmpty()) append(" • ")
                                append(node.member.relationshipToOwner)
                            }
                        }
                        PdfExportHelper.TreePdfNode(
                            node.member.id,
                            node.member.fullName,
                            sub,
                            node.x,
                            node.y,
                            node.generation
                        )
                    }
                    val pdfEdges = layout.edges.map { edge ->
                        PdfExportHelper.TreePdfEdge(
                            edge.fromX,
                            edge.fromY,
                            edge.toX,
                            edge.toY,
                            edge.kind == TreeEdgeKind.SPOUSE
                        )
                    }
                    val file = PdfExportHelper.generateFamilyTreePdf(context, pdfNodes, pdfEdges)
                    mediaStorageManager.getUriForFile(file)
                }
            }
            result.onSuccess(onReady)
                .onFailure { message.value = "Could not export tree PDF: ${it.message}" }
        }
    }

    fun clearMessage() {
        message.value = null
    }
}
