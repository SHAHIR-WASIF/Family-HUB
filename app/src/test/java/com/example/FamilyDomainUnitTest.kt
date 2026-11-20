package com.example

import com.example.data.local.entity.FamilyMemberEntity
import com.example.data.local.entity.RelationshipEntity
import com.example.data.local.entity.RelationshipType
import com.example.domain.DateUtils
import com.example.domain.FamilyTreeLayoutEngine
import com.example.domain.RelationshipLabelResolver
import com.example.domain.TreeEdgeKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FamilyDomainUnitTest {

    @Test
    fun `age and birthday countdown handle leap year and standard dates`() {
        val today = LocalDate.of(2026, 11, 20)
        assertEquals(36, DateUtils.calculateAgeYears("1990-05-10", today))
        assertEquals(0L, DateUtils.daysUntilNextOccurrence("1985-11-20", today))
        assertEquals(1L, DateUtils.daysUntilNextOccurrence("2000-11-21", today))
        // Leap day on non-leap year 2027 resolves cleanly without throwing
        assertNotNull(DateUtils.daysUntilNextOccurrence("2000-02-29", LocalDate.of(2027, 2, 1)))
    }

    @Test
    fun `tree layout organizes multi-generation spouses children and unlinked members safely`() {
        val gma = FamilyMemberEntity(id = 1, fullName = "Grandma Rose")
        val mom = FamilyMemberEntity(id = 2, fullName = "Mom Claire")
        val dad = FamilyMemberEntity(id = 3, fullName = "Dad Liam")
        val child = FamilyMemberEntity(id = 4, fullName = "Child Noah")
        val unlinked = FamilyMemberEntity(id = 5, fullName = "Uncle Sam", generationOffset = 0)

        val rels = listOf(
            RelationshipEntity(id = 1, fromMemberId = 2, toMemberId = 1, type = RelationshipType.MOTHER),
            RelationshipEntity(id = 2, fromMemberId = 2, toMemberId = 3, type = RelationshipType.SPOUSE),
            RelationshipEntity(id = 3, fromMemberId = 3, toMemberId = 2, type = RelationshipType.SPOUSE),
            RelationshipEntity(id = 4, fromMemberId = 4, toMemberId = 2, type = RelationshipType.MOTHER),
            RelationshipEntity(id = 5, fromMemberId = 4, toMemberId = 3, type = RelationshipType.FATHER)
        )

        val layout = FamilyTreeLayoutEngine.buildLayout(listOf(gma, mom, dad, child, unlinked), rels)
        assertEquals(5, layout.nodes.size)

        val genById = layout.nodes.associate { it.member.id to it.generation }
        assertTrue(genById[1]!! < genById[2]!!)
        assertEquals(genById[2], genById[3])
        assertTrue(genById[2]!! < genById[4]!!)
        assertTrue(layout.edges.any { it.kind == TreeEdgeKind.SPOUSE })
        assertTrue(layout.edges.any { it.kind == TreeEdgeKind.PARENT_CHILD })
    }

    @Test
    fun `relationship label resolver computes 1-hop and 2-hop labels to family owner`() {
        val owner = FamilyMemberEntity(id = 1, fullName = "Owner", gender = "Female")
        val mom = FamilyMemberEntity(id = 2, fullName = "Mom", gender = "Female")
        val gma = FamilyMemberEntity(id = 3, fullName = "Nana", gender = "Female")
        val rels = listOf(
            RelationshipEntity(fromMemberId = 1, toMemberId = 2, type = RelationshipType.MOTHER),
            RelationshipEntity(fromMemberId = 2, toMemberId = 3, type = RelationshipType.MOTHER)
        )

        assertEquals("You (Family Owner)", RelationshipLabelResolver.resolveLabelToOwner(owner, 1L, rels))
        assertEquals("Your Mother", RelationshipLabelResolver.resolveLabelToOwner(mom, 1L, rels))
        assertEquals("Your Grandmother", RelationshipLabelResolver.resolveLabelToOwner(gma, 1L, rels))
    }
}
