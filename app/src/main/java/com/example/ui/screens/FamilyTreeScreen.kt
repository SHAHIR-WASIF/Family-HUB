package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.domain.DateUtils
import com.example.domain.FamilyTreeLayoutEngine
import com.example.domain.TreeEdgeKind
import com.example.domain.TreeNodeLayout
import com.example.ui.components.EditorialEmptyState
import com.example.ui.components.MemberAvatar
import com.example.ui.components.shareExportedFile
import com.example.ui.viewmodel.FamilyTreeViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyTreeScreen(
    onOpenMember: (Long) -> Unit,
    onAddMember: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: FamilyTreeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var scale by remember { mutableFloatStateOf(0.9f) }
    var panOffset by remember { mutableStateOf(Offset(40f, 40f)) }

    LaunchedEffect(state.message) {
        state.message?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Family Tree") },
                actions = {
                    if (state.layout.nodes.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                viewModel.exportTreePdf { uri ->
                                    shareExportedFile(context, uri, "application/pdf", "Share Printable Family Tree PDF")
                                }
                            },
                            modifier = Modifier.testTag("export_tree_pdf_button")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "Export printable family tree PDF")
                        }
                    }
                    IconButton(onClick = onAddMember) {
                        Icon(Icons.Default.Add, contentDescription = "Add family member")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { innerPadding ->
        val layout = state.layout

        if (layout.nodes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                EditorialEmptyState(
                    drawableRes = R.drawable.img_empty_album,
                    title = "Build Your Family Tree",
                    subtitle = "Add family members and link parents, spouses, or children to visualize generations interactively.",
                    actionLabel = "Add First Member",
                    actionTag = "tree_empty_add_button",
                    onAction = onAddMember
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Generation Navigation Bar + Legend
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 1.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = state.selectedGenerationFilter == null,
                                onClick = {
                                    viewModel.selectGeneration(null)
                                    scale = 0.85f
                                    panOffset = Offset(30f, 30f)
                                },
                                label = { Text("All (${layout.nodes.size})") }
                            )
                        }
                        items(layout.generations, key = { "tree_gen_$it" }) { gen ->
                            val count = layout.nodes.count { it.generation == gen }
                            FilterChip(
                                selected = state.selectedGenerationFilter == gen,
                                onClick = {
                                    viewModel.selectGeneration(gen)
                                    val genNodes = layout.nodes.filter { it.generation == gen }
                                    if (genNodes.isNotEmpty()) {
                                        val avgY = genNodes.map { it.y }.average().toFloat()
                                        panOffset = Offset(40f, -avgY * scale + 260f)
                                    }
                                },
                                label = { Text("Gen ${gen + 1} ($count)") }
                            )
                        }
                    }
                }
            }

            // Interactive Pan & Zoom Tree Canvas
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.background)
                    .pointerInput(layout.nodes.size) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(0.35f, 2.4f)
                            scale = newScale
                            panOffset += pan
                        }
                    }
            ) {
                val density = LocalDensity.current
                val viewportW = with(density) { maxWidth.toPx() }
                val viewportH = with(density) { maxHeight.toPx() }

                val primaryColor = MaterialTheme.colorScheme.primary
                val secondaryColor = MaterialTheme.colorScheme.secondary
                val tertiaryColor = MaterialTheme.colorScheme.tertiary
                val gridDotColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)

                // Subtle background dot grid
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val spacing = 48f * scale.coerceIn(0.5f, 1.5f)
                    val startX = (panOffset.x % spacing)
                    val startY = (panOffset.y % spacing)
                    var gx = startX
                    while (gx < size.width) {
                        var gy = startY
                        while (gy < size.height) {
                            drawCircle(color = gridDotColor, radius = 2f, center = Offset(gx, gy))
                            gy += spacing
                        }
                        gx += spacing
                    }
                }

                // Transformed Tree Content Layer
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = panOffset.x
                            translationY = panOffset.y
                            scaleX = scale
                            scaleY = scale
                            transformOrigin = TransformOrigin(0f, 0f)
                        }
                ) {
                    // Edges Canvas
                    Canvas(
                        modifier = Modifier.size(
                            width = with(density) { layout.width.toDp() },
                            height = with(density) { layout.height.toDp() }
                        )
                    ) {
                        layout.edges.forEach { edge ->
                            when (edge.kind) {
                                TreeEdgeKind.PARENT_CHILD -> {
                                    val midY = (edge.fromY + edge.toY) / 2f
                                    val path = Path().apply {
                                        moveTo(edge.fromX, edge.fromY)
                                        cubicTo(
                                            edge.fromX, midY,
                                            edge.toX, midY,
                                            edge.toX, edge.toY
                                        )
                                    }
                                    drawPath(
                                        path = path,
                                        color = primaryColor.copy(alpha = 0.78f),
                                        style = Stroke(width = 3.2f)
                                    )
                                }
                                TreeEdgeKind.SPOUSE -> {
                                    drawLine(
                                        color = secondaryColor,
                                        start = Offset(edge.fromX, edge.fromY - 2f),
                                        end = Offset(edge.toX, edge.toY - 2f),
                                        strokeWidth = 3f
                                    )
                                    drawLine(
                                        color = secondaryColor,
                                        start = Offset(edge.fromX, edge.fromY + 2f),
                                        end = Offset(edge.toX, edge.toY + 2f),
                                        strokeWidth = 3f
                                    )
                                }
                                TreeEdgeKind.SIBLING, TreeEdgeKind.OTHER -> {
                                    drawLine(
                                        color = tertiaryColor.copy(alpha = 0.65f),
                                        start = Offset(edge.fromX, edge.fromY),
                                        end = Offset(edge.toX, edge.toY),
                                        strokeWidth = 2.2f,
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                                    )
                                }
                            }
                        }
                    }

                    // Interactive Node Cards
                    layout.nodes.forEach { node ->
                        val nodeW = FamilyTreeLayoutEngine.NODE_WIDTH
                        val nodeH = FamilyTreeLayoutEngine.NODE_HEIGHT
                        val leftPx = (node.x - nodeW / 2f).roundToInt()
                        val topPx = (node.y - nodeH / 2f).roundToInt()
                        val isHighlighted = state.selectedGenerationFilter == null ||
                            state.selectedGenerationFilter == node.generation

                        TreePersonCard(
                            node = node,
                            isFocused = node.member.id == state.focusMemberId,
                            dimmed = !isHighlighted,
                            onClick = { onOpenMember(node.member.id) },
                            modifier = Modifier
                                .offset { IntOffset(leftPx, topPx) }
                                .size(
                                    width = with(density) { nodeW.toDp() },
                                    height = with(density) { nodeH.toDp() }
                                )
                        )
                    }
                }

                // Legend & Zoom Controls
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LegendItem(color = primaryColor, label = "Parent–Child")
                        LegendItem(color = secondaryColor, label = "Spouse")
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalIconButton(
                        onClick = { scale = (scale * 1.2f).coerceAtMost(2.4f) },
                        modifier = Modifier.testTag("tree_zoom_in")
                    ) {
                        Icon(Icons.Default.ZoomIn, contentDescription = "Zoom in")
                    }
                    FilledTonalIconButton(
                        onClick = { scale = (scale / 1.2f).coerceAtLeast(0.35f) },
                        modifier = Modifier.testTag("tree_zoom_out")
                    ) {
                        Icon(Icons.Default.ZoomOut, contentDescription = "Zoom out")
                    }
                    FilledTonalIconButton(
                        onClick = {
                            val fitScale = minOf(
                                viewportW / layout.width.coerceAtLeast(1f),
                                viewportH / layout.height.coerceAtLeast(1f)
                            ).coerceIn(0.45f, 1.1f)
                            scale = fitScale
                            panOffset = Offset(
                                (viewportW - layout.width * fitScale) / 2f,
                                24f
                            )
                        },
                        modifier = Modifier.testTag("tree_fit_screen")
                    ) {
                        Icon(Icons.Default.CenterFocusStrong, contentDescription = "Fit tree to screen")
                    }
                }
            }
        }
    }
}

@Composable
private fun TreePersonCard(
    node: TreeNodeLayout,
    isFocused: Boolean,
    dimmed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val m = node.member
    val borderColor = if (isFocused) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .border(if (isFocused) 2.5.dp else 1.2.dp, borderColor, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .graphicsLayer { alpha = if (dimmed) 0.38f else 1f }
            .testTag("tree_node_${m.id}"),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        tonalElevation = 3.dp,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            MemberAvatar(
                fullName = m.fullName,
                photoPath = m.profilePhotoPath,
                memberId = m.id,
                size = 38.dp
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = m.fullName,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            val sub = buildString {
                DateUtils.extractBirthYear(m.dateOfBirth)?.let { append("b. $it") }
                if (!m.relationshipToOwner.isNullOrBlank()) {
                    if (isNotEmpty()) append(" • ")
                    append(m.relationshipToOwner)
                }
            }.ifBlank { "Gen ${node.generation + 1}" }
            Text(
                text = sub,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LegendItem(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(width = 14.dp, height = 4.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(6.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}
