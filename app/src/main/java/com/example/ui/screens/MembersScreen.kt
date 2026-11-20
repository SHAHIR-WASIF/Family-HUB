package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.domain.DateUtils
import com.example.ui.components.EditorialEmptyState
import com.example.ui.components.MemberAvatar
import com.example.ui.viewmodel.MemberListEntry
import com.example.ui.viewmodel.MemberSortOrder
import com.example.ui.viewmodel.MembersViewModel
import com.example.ui.viewmodel.RelationshipFilter

@Composable
fun MembersScreen(
    onOpenMember: (Long) -> Unit,
    onAddMember: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: MembersViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddMember,
                modifier = Modifier.testTag("add_member_fab"),
                icon = { Icon(Icons.Default.Add, contentDescription = "Add family member") },
                text = { Text("Add Member") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
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
                    .widthIn(max = 960.dp)
                    .padding(horizontal = 16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Family Members",
                            style = MaterialTheme.typography.headlineLarge
                        )
                        Text(
                            text = "${state.members.size} of ${state.totalCount} shown",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box {
                            IconButton(
                                onClick = { showSortMenu = true },
                                modifier = Modifier.testTag("members_sort_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort members")
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                MemberSortOrder.entries.forEach { order ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = if (state.sortOrder == order) "✓ ${order.label}" else order.label
                                            )
                                        },
                                        onClick = {
                                            viewModel.onSortOrderChange(order)
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }
                        IconButton(
                            onClick = viewModel::toggleLayoutMode,
                            modifier = Modifier.testTag("members_layout_toggle")
                        ) {
                            Icon(
                                imageVector = if (state.isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                                contentDescription = if (state.isGridView) "Switch to list view" else "Switch to grid view"
                            )
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                        }
                    }
                }

                // Search bar
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("members_search_input"),
                    placeholder = { Text("Search name, nickname, occupation, birthplace…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp)
                )

                Spacer(Modifier.height(10.dp))

                // Filter chips: Gender + Relationship + Generation
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val genders = listOf(null to "All Genders", "Female" to "Female", "Male" to "Male", "Other" to "Other")
                    items(genders, key = { "g_${it.first ?: "all"}" }) { (value, label) ->
                        FilterChip(
                            selected = state.selectedGender == value,
                            onClick = { viewModel.onGenderFilterChange(value) },
                            label = { Text(label) }
                        )
                    }

                    items(RelationshipFilter.entries, key = { "rf_${it.name}" }) { relFilter ->
                        if (relFilter != RelationshipFilter.ALL) {
                            FilterChip(
                                selected = state.relationshipFilter == relFilter,
                                onClick = {
                                    viewModel.onRelationshipFilterChange(
                                        if (state.relationshipFilter == relFilter) RelationshipFilter.ALL else relFilter
                                    )
                                },
                                label = { Text(relFilter.label) }
                            )
                        }
                    }

                    if (state.availableGenerations.size > 1) {
                        items(state.availableGenerations, key = { "gen_$it" }) { gen ->
                            FilterChip(
                                selected = state.selectedGeneration == gen,
                                onClick = {
                                    viewModel.onGenerationFilterChange(if (state.selectedGeneration == gen) null else gen)
                                },
                                label = { Text("Gen ${gen + 1}") }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                if (state.totalCount == 0) {
                    EditorialEmptyState(
                        drawableRes = R.drawable.img_empty_album,
                        title = "No Family Members Yet",
                        subtitle = "Add your first family member with just a name, or import a JSON backup from Settings.",
                        actionLabel = "Add First Member",
                        actionTag = "members_empty_add_button",
                        onAction = onAddMember
                    )
                } else if (state.members.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No matching profiles",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Try adjusting your search or filter chips.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        TextButton(onClick = {
                            viewModel.onSearchQueryChange("")
                            viewModel.onGenderFilterChange(null)
                            viewModel.onGenerationFilterChange(null)
                            viewModel.onRelationshipFilterChange(RelationshipFilter.ALL)
                        }) {
                            Text("Reset Filters")
                        }
                    }
                } else if (state.isGridView) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 162.dp),
                        contentPadding = PaddingValues(bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.members, key = { it.member.id }) { entry ->
                            MemberGridCard(
                                entry = entry,
                                onClick = { onOpenMember(entry.member.id) }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.members, key = { it.member.id }) { entry ->
                            MemberListRowCard(
                                entry = entry,
                                onClick = { onOpenMember(entry.member.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberGridCard(
    entry: MemberListEntry,
    onClick: () -> Unit
) {
    val m = entry.member
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("member_grid_card_${m.id}"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                MemberAvatar(
                    fullName = m.fullName,
                    photoPath = m.profilePhotoPath,
                    memberId = m.id,
                    size = 70.dp,
                    modifier = Modifier.align(Alignment.Center)
                )
                if (m.isPinned || m.isFavorite) {
                    Icon(
                        imageVector = if (m.isPinned) Icons.Default.PushPin else Icons.Default.Favorite,
                        contentDescription = "Pinned or favorite",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.TopEnd)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = m.fullName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            m.nickname?.takeIf { it.isNotBlank() }?.let { nick ->
                Text(
                    text = "\"$nick\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(6.dp))
            val ageText = DateUtils.formatAgeLabel(m.dateOfBirth)
            val meta = listOfNotNull(entry.relationshipBadge, ageText, "Gen ${entry.generation + 1}")
                .firstOrNull() ?: "Gen ${entry.generation + 1}"
            Surface(
                shape = RoundedCornerShape(50.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = meta,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun MemberListRowCard(
    entry: MemberListEntry,
    onClick: () -> Unit
) {
    val m = entry.member
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("member_list_card_${m.id}"),
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
                fullName = m.fullName,
                photoPath = m.profilePhotoPath,
                memberId = m.id,
                size = 54.dp
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = m.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (m.isPinned || m.isFavorite) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = if (m.isPinned) Icons.Default.PushPin else Icons.Default.Favorite,
                            contentDescription = "Highlighted",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
                val subParts = listOfNotNull(
                    entry.relationshipBadge,
                    DateUtils.formatAgeLabel(m.dateOfBirth),
                    m.occupation?.takeIf { it.isNotBlank() }
                )
                Text(
                    text = if (subParts.isNotEmpty()) subParts.joinToString(" • ") else "Generation ${entry.generation + 1}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                shape = RoundedCornerShape(50.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Text(
                    text = "Gen ${entry.generation + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
