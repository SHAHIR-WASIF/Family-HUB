package com.example.domain

import com.example.data.local.entity.FamilyMemberEntity
import com.example.data.local.entity.RelationshipEntity
import com.example.data.local.entity.RelationshipType

/**
 * Computes automatic human-readable relationship labels relative to the selected Family Owner ("Me").
 */
object RelationshipLabelResolver {

    fun resolveLabelToOwner(
        targetMember: FamilyMemberEntity,
        ownerId: Long?,
        relationships: List<RelationshipEntity>
    ): String? {
        if (!targetMember.relationshipToOwner.isNullOrBlank()) {
            return targetMember.relationshipToOwner
        }
        if (ownerId == null) return null
        if (targetMember.id == ownerId) return "You (Family Owner)"

        val direct = relationships.firstOrNull {
            it.fromMemberId == ownerId && it.toMemberId == targetMember.id
        }
        if (direct != null) {
            return when (direct.type) {
                RelationshipType.FATHER -> "Your Father"
                RelationshipType.MOTHER -> "Your Mother"
                RelationshipType.SPOUSE -> "Your Spouse"
                RelationshipType.CHILD -> when (targetMember.gender?.lowercase()) {
                    "male" -> "Your Son"
                    "female" -> "Your Daughter"
                    else -> "Your Child"
                }
                RelationshipType.BROTHER -> "Your Brother"
                RelationshipType.SISTER -> "Your Sister"
                RelationshipType.GRANDPARENT -> when (targetMember.gender?.lowercase()) {
                    "male" -> "Your Grandfather"
                    "female" -> "Your Grandmother"
                    else -> "Your Grandparent"
                }
                RelationshipType.OTHER_RELATIVE -> direct.customLabel ?: "Relative"
            }
        }

        // Check 2-hop inferences (e.g. parent's parent = grandparent, child's child = grandchild)
        val ownerOutgoing = relationships.filter { it.fromMemberId == ownerId }
        val ownerParents = ownerOutgoing.filter {
            it.type == RelationshipType.FATHER || it.type == RelationshipType.MOTHER
        }.map { it.toMemberId }

        val ownerChildren = ownerOutgoing.filter { it.type == RelationshipType.CHILD }.map { it.toMemberId }

        // Parent's parent -> Grandparent
        val isGrandparent = relationships.any { rel ->
            rel.fromMemberId in ownerParents &&
                rel.toMemberId == targetMember.id &&
                (rel.type == RelationshipType.FATHER || rel.type == RelationshipType.MOTHER)
        }
        if (isGrandparent) {
            return when (targetMember.gender?.lowercase()) {
                "male" -> "Your Grandfather"
                "female" -> "Your Grandmother"
                else -> "Your Grandparent"
            }
        }

        // Child's child -> Grandchild
        val isGrandchild = relationships.any { rel ->
            rel.fromMemberId in ownerChildren &&
                rel.toMemberId == targetMember.id &&
                rel.type == RelationshipType.CHILD
        }
        if (isGrandchild) {
            return when (targetMember.gender?.lowercase()) {
                "male" -> "Your Grandson"
                "female" -> "Your Granddaughter"
                else -> "Your Grandchild"
            }
        }

        // Parent's sibling -> Uncle / Aunt
        val isAuntOrUncle = relationships.any { rel ->
            rel.fromMemberId in ownerParents &&
                rel.toMemberId == targetMember.id &&
                (rel.type == RelationshipType.BROTHER || rel.type == RelationshipType.SISTER)
        }
        if (isAuntOrUncle) {
            return when (targetMember.gender?.lowercase()) {
                "male" -> "Your Uncle"
                "female" -> "Your Aunt"
                else -> "Your Aunt / Uncle"
            }
        }

        // Sibling's child -> Nephew / Niece
        val ownerSiblings = ownerOutgoing.filter {
            it.type == RelationshipType.BROTHER || it.type == RelationshipType.SISTER
        }.map { it.toMemberId }
        val isNieceOrNephew = relationships.any { rel ->
            rel.fromMemberId in ownerSiblings &&
                rel.toMemberId == targetMember.id &&
                rel.type == RelationshipType.CHILD
        }
        if (isNieceOrNephew) {
            return when (targetMember.gender?.lowercase()) {
                "male" -> "Your Nephew"
                "female" -> "Your Niece"
                else -> "Your Niece / Nephew"
            }
        }

        return null
    }
}
