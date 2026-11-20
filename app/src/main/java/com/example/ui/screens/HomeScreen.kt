package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.entity.FamilyMemberEntity
import com.example.domain.DateUtils
import com.example.ui.components.EditorialEmptyState
import com.example.ui.components.HeroGradientOverlay
import com.example.ui.components.MemberAvatar
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatTile
import com.example.ui.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    onOpenMember: (Long) -> Unit,
    onAddMember: () -> Unit,
    onOpenMembers: () -> Unit,
    onOpenTree: () -> Unit,
    onOpenEvents: () -> Unit,
    onOpenGallery: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 880.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Family Hub",
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Preserve stories, lineages & shared moments",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("home_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Hero Heritage Banner
            item {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    tonalElevation = 4.dp,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_heritage_banner),
                            contentDescription = "Family heritage botanical banner",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        HeroGradientOverlay(modifier = Modifier.fillMaxSize())
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Surface(
                                    color = Color.White.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(50.dp)
                                ) {
                                    Text(
                                        text = "OFFLINE FAMILY ARCHIVE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (state.totalMembers == 0) "Start Your Family Story"
                                    else "${state.totalMembers} Family ${if (state.totalMembers == 1) "Member" else "Members"}",
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = Color.White
                                )
                                Text(
                                    text = if (state.totalMembers == 0) "Private, offline & forever yours on this device."
                                    else "${state.statistics.generationCount} generations • ${state.statistics.totalPhotos} photos • ${state.statistics.totalEvents} events",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.88f)
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = onAddMember,
                                    modifier = Modifier.testTag("hero_add_member_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Add Member")
                                }
                                FilledTonalButton(
                                    onClick = onOpenTree,
                                    modifier = Modifier.testTag("hero_tree_button"),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = Color.White.copy(alpha = 0.2f),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Default.AccountTree, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Family Tree")
                                }
                            }
                        }
                    }
                }
            }

            // Quick Actions Strip
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        QuickActionPill(
                            icon = Icons.Default.Add,
                            label = "New Profile",
                            onClick = onAddMember,
                            tag = "quick_add_member"
                        )
                    }
                    item {
                        QuickActionPill(
                            icon = Icons.Default.AccountTree,
                            label = "Tree View",
                            onClick = onOpenTree,
                            tag = "quick_tree"
                        )
                    }
                    item {
                        QuickActionPill(
                            icon = Icons.Default.Celebration,
                            label = "Events",
                            onClick = onOpenEvents,
                            tag = "quick_events"
                        )
                    }
                    item {
                        QuickActionPill(
                            icon = Icons.Default.PhotoLibrary,
                            label = "Gallery",
                            onClick = onOpenGallery,
                            tag = "quick_gallery"
                        )
                    }
                    item {
                        QuickActionPill(
                            icon = Icons.Outlined.Backup,
                            label = "Backup",
                            onClick = onOpenSettings,
                            tag = "quick_backup"
                        )
                    }
                }
            }

            // Empty State if no members yet
            if (!state.isLoading && state.totalMembers == 0) {
                item {
                    EditorialEmptyState(
                        drawableRes = R.drawable.img_empty_album,
                        title = "Your Family Album Awaits",
                        subtitle = "Create a profile in seconds with just a name, then link relatives, milestones, and treasured photos.",
                        actionLabel = "Create First Profile",
                        actionTag = "empty_create_first_profile",
                        onAction = onAddMember
                    )
                }
            } else {
                // Summary Stat Tiles
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatTile(
                            icon = Icons.Default.Groups,
                            value = state.statistics.totalMembers.toString(),
                            label = "Members",
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        StatTile(
                            icon = Icons.Default.AccountTree,
                            value = state.statistics.generationCount.toString(),
                            label = "Generations",
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        StatTile(
                            icon = Icons.Default.PhotoLibrary,
                            value = state.statistics.totalPhotos.toString(),
                            label = "Photos",
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Pinned & Favorites
                if (state.pinnedAndFavorites.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Pinned & Favorites",
                            actionLabel = "All Members",
                            onActionClick = onOpenMembers
                        )
                        Spacer(Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(state.pinnedAndFavorites, key = { "pin_${it.id}" }) { member ->
                                MemberHorizontalCard(
                                    member = member,
                                    badge = state.ownerLabels[member.id],
                                    onClick = { onOpenMember(member.id) }
                                )
                            }
                        }
                    }
                }

                // Upcoming Birthdays & Events
                if (state.upcomingBirthdays.isNotEmpty() || state.upcomingEvents.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Upcoming Celebrations",
                            actionLabel = "Calendar",
                            onActionClick = onOpenEvents
                        )
                        Spacer(Modifier.height(10.dp))
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
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                state.upcomingBirthdays.take(4).forEach { bday ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable { onOpenMember(bday.member.id) }
                                            .padding(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        MemberAvatar(
                                            fullName = bday.member.fullName,
                                            photoPath = bday.member.profilePhotoPath,
                                            memberId = bday.member.id,
                                            size = 44.dp
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = bday.member.fullName,
                                                style = MaterialTheme.typography.titleMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            val turning = bday.turningAge?.let { " • Turning $it" } ?: ""
                                            Text(
                                                text = "${bday.displayDate}$turning",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(50.dp),
                                            color = if (bday.daysUntil == 0L) MaterialTheme.colorScheme.secondary
                                            else MaterialTheme.colorScheme.secondaryContainer
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Cake,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp),
                                                    tint = if (bday.daysUntil == 0L) MaterialTheme.colorScheme.onSecondary
                                                    else MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    text = when (bday.daysUntil) {
                                                        0L -> "Today!"
                                                        1L -> "Tomorrow"
                                                        else -> "In ${bday.daysUntil}d"
                                                    },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (bday.daysUntil == 0L) MaterialTheme.colorScheme.onSecondary
                                                    else MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }
                                        }
                                    }
                                }

                                state.upcomingEvents.take(3).forEach { (ev, days) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable(onClick = onOpenEvents)
                                            .padding(6.dp),
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
                                                imageVector = Icons.Default.Celebration,
                                                contentDescription = ev.title,
                                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                                            )
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = ev.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${ev.eventType.displayName} • ${DateUtils.formatDisplayDate(ev.eventDateIso)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(50.dp),
                                            color = MaterialTheme.colorScheme.tertiaryContainer
                                        ) {
                                            Text(
                                                text = when (days) {
                                                    0L -> "Today"
                                                    1L -> "Tomorrow"
                                                    else -> "In ${days}d"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Recently Added
                if (state.recentlyAdded.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Recently Added",
                            actionLabel = "See All",
                            onActionClick = onOpenMembers
                        )
                        Spacer(Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(state.recentlyAdded, key = { "add_${it.id}" }) { member ->
                                MemberHorizontalCard(
                                    member = member,
                                    badge = state.ownerLabels[member.id],
                                    onClick = { onOpenMember(member.id) }
                                )
                            }
                        }
                    }
                }

                // Recently Updated
                if (state.recentlyUpdated.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Recently Updated")
                        Spacer(Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(state.recentlyUpdated, key = { "upd_${it.id}" }) { member ->
                                MemberHorizontalCard(
                                    member = member,
                                    badge = "Updated ${DateUtils.formatTimestamp(member.updatedAt)}",
                                    onClick = { onOpenMember(member.id) }
                                )
                            }
                        }
                    }
                }

                // Recently Viewed
                if (state.recentlyViewed.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Recently Viewed")
                        Spacer(Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(state.recentlyViewed, key = { "view_${it.id}" }) { member ->
                                MemberHorizontalCard(
                                    member = member,
                                    badge = member.nickname?.takeIf { it.isNotBlank() } ?: member.occupation,
                                    onClick = { onOpenMember(member.id) }
                                )
                            }
                        }
                    }
                }

                // Family Statistics Breakdown
                item {
                    SectionHeader(title = "Family Insights")
                    Spacer(Modifier.height(10.dp))
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            state.statistics.averageAgeYears?.let { avg ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Average Family Age",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "$avg years",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            state.statistics.genderBreakdown.forEach { (label, count) ->
                                val ratio = if (state.totalMembers > 0) count.toFloat() / state.totalMembers else 0f
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = label, style = MaterialTheme.typography.bodySmall)
                                        Text(
                                            text = "$count (${(ratio * 100).toInt()}%)",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { ratio },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(7.dp)
                                            .clip(RoundedCornerShape(50.dp)),
                                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun QuickActionPill(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tag: String
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.testTag(tag),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun MemberHorizontalCard(
    member: FamilyMemberEntity,
    badge: String?,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier
            .width(156.dp)
            .testTag("home_member_card_${member.id}"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                MemberAvatar(
                    fullName = member.fullName,
                    photoPath = member.profilePhotoPath,
                    memberId = member.id,
                    size = 64.dp,
                    modifier = Modifier.align(Alignment.Center)
                )
                if (member.isPinned || member.isFavorite) {
                    Icon(
                        imageVector = if (member.isPinned) Icons.Default.PushPin else Icons.Default.Favorite,
                        contentDescription = if (member.isPinned) "Pinned" else "Favorite",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.TopEnd)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = member.fullName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val sub = badge?.takeIf { it.isNotBlank() }
                ?: DateUtils.formatAgeLabel(member.dateOfBirth)
                ?: "Profile"
            Text(
                text = sub,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
