package com.example.pedalpalooza.data

import kotlinx.serialization.Serializable

@Serializable
data class ShiftResponse(
    val events: List<ShiftEvent>,
    val pagination: Pagination
)

@Serializable
data class ShiftEvent(
    val id: String,
    val title: String,
    val venue: String? = null,
    val address: String? = null,
    val organizer: String? = null,
    val details: String? = null,
    val time: String? = null,
    val date: String? = null,
    val ridelength: String? = null,
    val locend: String? = null,
    val shareable: String? = null,
    val exportable: String? = null,
    val cancelled: Boolean = false
)

@Serializable
data class Pagination(
    val offset: Int,
    val limit: Int,
    val fullcount: Int
)
