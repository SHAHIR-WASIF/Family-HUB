package com.example.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data object MembersRoute

@Serializable
data object TreeRoute

@Serializable
data object EventsRoute

@Serializable
data object GalleryRoute

@Serializable
data object SettingsRoute

@Serializable
data class MemberDetailRoute(val memberId: Long)

@Serializable
data class AddEditMemberRoute(val memberId: Long = -1L)
