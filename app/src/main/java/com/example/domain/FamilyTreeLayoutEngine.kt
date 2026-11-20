package com.example.domain

import com.example.data.local.entity.FamilyMemberEntity
import com.example.data.local.entity.RelationshipEntity
import com.example.data.local.entity.RelationshipType
import kotlin.math.max

enum class TreeEdgeKind {
    PARENT_CHILD,
    SPOUSE,
    SIBLING,
    OTHER
}

data class TreeNodeLayout(
    val member: FamilyMemberEntity,
    val generation: Int,
    val x: Float,
    val y: Float
)

data class TreeEdgeLayout(
    val fromId: Long,
    val toId: Long,
    val fromX: Float,
    val fromY: Float,
    val toX: Float,
    val toY: Float,
    val kind: TreeEdgeKind
)

data class TreeLayoutResult(
    val nodes: List<TreeNodeLayout>,
    val edges: List<TreeEdgeLayout>,
    val generations: List<Int>,
    val width: Float,
    val height: Float
)

/**
 * Pure graph layout engine that organizes family members into generational tiers,
 * pairs spouses side-by-side, handles incomplete or disconnected trees safely,
 * and prevents cyclic relationship loops.
 */
object FamilyTreeLayoutEngine {
    const val NODE_WIDTH = 172f
    const val NODE_HEIGHT = 112f
    const val HORIZONTAL_GAP = 42f
    const val SPOUSE_GAP = 26f
    const val VERTICAL_GAP = 104f
    const val PADDING = 120f

    fun buildLayout(
        members: List<FamilyMemberEntity>,
        relationships: List<RelationshipEntity>,
        focusMemberId: Long? = null
    ): TreeLayoutResult {
        if (members.isEmpty()) {
            return TreeLayoutResult(emptyList(), emptyList(), emptyList(), 600f, 400f)
        }

        val memberMap = members.associateBy { it.id }
        val genMap = mutableMapOf<Long, Int>()

        // Build normalized undirected/directed adjacency for generation propagation
        // delta(from -> to): FATHER/MOTHER => -1, GRANDPARENT => -2, CHILD => +1, SPOUSE/BROTHER/SISTER => 0
        val directedDeltas = mutableMapOf<Long, MutableList<Pair<Long, Int>>>()
        fun addDelta(from: Long, to: Long, delta: Int) {
            if (from != to && memberMap.containsKey(from) && memberMap.containsKey(to)) {
                directedDeltas.getOrPut(from) { mutableListOf() }.add(to to delta)
            }
        }

        relationships.forEach { rel ->
            when (rel.type) {
                RelationshipType.FATHER, RelationshipType.MOTHER -> {
                    addDelta(rel.fromMemberId, rel.toMemberId, -1)
                    addDelta(rel.toMemberId, rel.fromMemberId, +1)
                }
                RelationshipType.CHILD -> {
                    addDelta(rel.fromMemberId, rel.toMemberId, +1)
                    addDelta(rel.toMemberId, rel.fromMemberId, -1)
                }
                RelationshipType.GRANDPARENT -> {
                    addDelta(rel.fromMemberId, rel.toMemberId, -2)
                    addDelta(rel.toMemberId, rel.fromMemberId, +2)
                }
                RelationshipType.SPOUSE, RelationshipType.BROTHER, RelationshipType.SISTER, RelationshipType.OTHER_RELATIVE -> {
                    addDelta(rel.fromMemberId, rel.toMemberId, 0)
                    addDelta(rel.toMemberId, rel.fromMemberId, 0)
                }
            }
        }

        // BFS from focusMemberId or first connected member, then cover all disconnected components
        val orderedSeeds = buildList {
            focusMemberId?.let { if (memberMap.containsKey(it)) add(it) }
            members.forEach { if (it.id != focusMemberId) add(it.id) }
        }

        orderedSeeds.forEach { seedId ->
            if (!genMap.containsKey(seedId)) {
                val seedMember = memberMap.getValue(seedId)
                genMap[seedId] = seedMember.generationOffset
                val queue = ArrayDeque<Long>()
                queue.add(seedId)
                var steps = 0
                val maxSteps = members.size * 6
                while (queue.isNotEmpty() && steps < maxSteps) {
                    steps++
                    val current = queue.removeFirst()
                    val currentGen = genMap.getValue(current)
                    directedDeltas[current].orEmpty().forEach { (neighbor, delta) ->
                        if (!genMap.containsKey(neighbor)) {
                            genMap[neighbor] = currentGen + delta
                            queue.add(neighbor)
                        }
                    }
                }
            }
        }

        // Normalize generations so top-most generation is 0
        val minGen = genMap.values.minOrNull() ?: 0
        val normalizedGen = genMap.mapValues { (_, g) -> g - minGen }

        // Group into generation rows; keep spouses adjacent within the same row
        val spouseLookup = mutableMapOf<Long, MutableSet<Long>>()
        relationships.filter { it.type == RelationshipType.SPOUSE }.forEach { rel ->
            if (memberMap.containsKey(rel.fromMemberId) && memberMap.containsKey(rel.toMemberId)) {
                spouseLookup.getOrPut(rel.fromMemberId) { mutableSetOf() }.add(rel.toMemberId)
                spouseLookup.getOrPut(rel.toMemberId) { mutableSetOf() }.add(rel.fromMemberId)
            }
        }

        // Parent lookup for horizontal sorting alignment
        val parentsOf = mutableMapOf<Long, MutableSet<Long>>()
        relationships.forEach { rel ->
            when (rel.type) {
                RelationshipType.FATHER, RelationshipType.MOTHER -> {
                    parentsOf.getOrPut(rel.fromMemberId) { mutableSetOf() }.add(rel.toMemberId)
                }
                RelationshipType.CHILD -> {
                    parentsOf.getOrPut(rel.toMemberId) { mutableSetOf() }.add(rel.fromMemberId)
                }
                else -> Unit
            }
        }

        val generations = normalizedGen.values.distinct().sorted()
        val positionMap = mutableMapOf<Long, Pair<Float, Float>>()

        // First pass: build ordered units per generation (pairing spouses side by side)
        val unitsByGen = mutableMapOf<Int, List<List<FamilyMemberEntity>>>()
        generations.forEach { gen ->
            val rowMembers = members.filter { normalizedGen[it.id] == gen }
                .sortedWith(compareBy({ it.dateOfBirth ?: "9999" }, { it.fullName.lowercase() }))

            val placed = mutableSetOf<Long>()
            val units = mutableListOf<List<FamilyMemberEntity>>()

            rowMembers.forEach { m ->
                if (m.id !in placed) {
                    placed.add(m.id)
                    val unit = mutableListOf(m)
                    val spousesInSameGen = spouseLookup[m.id].orEmpty()
                        .mapNotNull { memberMap[it] }
                        .filter { it.id !in placed && normalizedGen[it.id] == gen }
                    spousesInSameGen.forEach { spouse ->
                        placed.add(spouse.id)
                        unit.add(spouse)
                    }
                    units.add(unit)
                }
            }

            // Sort units by average parent X if parents already positioned
            val sortedUnits = if (gen == generations.first()) {
                units
            } else {
                units.sortedBy { unit ->
                    val parentXs = unit.flatMap { m -> parentsOf[m.id].orEmpty() }
                        .mapNotNull { positionMap[it]?.first }
                    if (parentXs.isNotEmpty()) parentXs.average().toFloat() else Float.MAX_VALUE
                }
            }

            unitsByGen[gen] = sortedUnits

            // Compute tentative X coordinates for this row so child rows can align beneath parents
            var cursorX = PADDING
            val y = PADDING + gen * (NODE_HEIGHT + VERTICAL_GAP) + NODE_HEIGHT / 2f
            sortedUnits.forEach { unit ->
                unit.forEachIndexed { idx, member ->
                    val cx = cursorX + NODE_WIDTH / 2f
                    positionMap[member.id] = cx to y
                    cursorX += NODE_WIDTH + if (idx < unit.lastIndex) SPOUSE_GAP else HORIZONTAL_GAP
                }
            }
        }

        // Second pass: center each row horizontally relative to the widest row
        val rowWidths = generations.associateWith { gen ->
            val units = unitsByGen[gen].orEmpty()
            if (units.isEmpty()) 0f
            else {
                var w = 0f
                units.forEachIndexed { uIdx, unit ->
                    unit.forEachIndexed { mIdx, _ ->
                        w += NODE_WIDTH
                        if (mIdx < unit.lastIndex) w += SPOUSE_GAP
                    }
                    if (uIdx < units.lastIndex) w += HORIZONTAL_GAP
                }
                w
            }
        }
        val maxRowWidth = max(480f, rowWidths.values.maxOrNull() ?: 480f)

        generations.forEach { gen ->
            val rowW = rowWidths[gen] ?: 0f
            val shiftX = (maxRowWidth - rowW) / 2f
            val rowMembers = unitsByGen[gen].orEmpty().flatten()
            rowMembers.forEach { m ->
                val (cx, cy) = positionMap.getValue(m.id)
                positionMap[m.id] = (cx + shiftX) to cy
            }
        }

        val nodeLayouts = members.map { member ->
            val (cx, cy) = positionMap[member.id] ?: (PADDING to PADDING)
            TreeNodeLayout(
                member = member,
                generation = normalizedGen[member.id] ?: 0,
                x = cx,
                y = cy
            )
        }

        // Build deduplicated visual edges
        val edges = mutableListOf<TreeEdgeLayout>()
        val seenPairs = mutableSetOf<String>()

        // Parent -> Child edges
        parentsOf.forEach { (childId, parentSet) ->
            val childPos = positionMap[childId] ?: return@forEach
            parentSet.forEach { parentId ->
                val parentPos = positionMap[parentId] ?: return@forEach
                val key = "P:$parentId->$childId"
                if (seenPairs.add(key)) {
                    edges.add(
                        TreeEdgeLayout(
                            fromId = parentId,
                            toId = childId,
                            fromX = parentPos.first,
                            fromY = parentPos.second + NODE_HEIGHT / 2f,
                            toX = childPos.first,
                            toY = childPos.second - NODE_HEIGHT / 2f,
                            kind = TreeEdgeKind.PARENT_CHILD
                        )
                    )
                }
            }
        }

        // Spouse edges
        spouseLookup.forEach { (aId, bSet) ->
            val posA = positionMap[aId] ?: return@forEach
            bSet.forEach { bId ->
                val posB = positionMap[bId] ?: return@forEach
                val low = minOf(aId, bId)
                val high = maxOf(aId, bId)
                val key = "S:$low-$high"
                if (seenPairs.add(key)) {
                    val leftIsA = posA.first <= posB.first
                    val leftPos = if (leftIsA) posA else posB
                    val rightPos = if (leftIsA) posB else posA
                    edges.add(
                        TreeEdgeLayout(
                            fromId = low,
                            toId = high,
                            fromX = leftPos.first + NODE_WIDTH / 2f,
                            fromY = leftPos.second,
                            toX = rightPos.first - NODE_WIDTH / 2f,
                            toY = rightPos.second,
                            kind = TreeEdgeKind.SPOUSE
                        )
                    )
                }
            }
        }

        // Sibling & Other edges (only if neither member shares a visible parent edge so the chart stays uncluttered)
        relationships.forEach { rel ->
            if (rel.type == RelationshipType.BROTHER || rel.type == RelationshipType.SISTER || rel.type == RelationshipType.OTHER_RELATIVE) {
                val aId = rel.fromMemberId
                val bId = rel.toMemberId
                val posA = positionMap[aId] ?: return@forEach
                val posB = positionMap[bId] ?: return@forEach
                val low = minOf(aId, bId)
                val high = maxOf(aId, bId)
                val shareParent = parentsOf[aId]?.intersect(parentsOf[bId].orEmpty())?.isNotEmpty() == true
                if (!shareParent) {
                    val key = "O:$low-$high"
                    if (seenPairs.add(key)) {
                        edges.add(
                            TreeEdgeLayout(
                                fromId = low,
                                toId = high,
                                fromX = posA.first,
                                fromY = posA.second,
                                toX = posB.first,
                                toY = posB.second,
                                kind = if (rel.type == RelationshipType.OTHER_RELATIVE) TreeEdgeKind.OTHER else TreeEdgeKind.SIBLING
                            )
                        )
                    }
                }
            }
        }

        val totalWidth = maxRowWidth + PADDING * 2f
        val totalHeight = PADDING * 2f + max(1, generations.size) * (NODE_HEIGHT + VERTICAL_GAP)
        return TreeLayoutResult(
            nodes = nodeLayouts,
            edges = edges,
            generations = generations,
            width = totalWidth,
            height = totalHeight
        )
    }
}
