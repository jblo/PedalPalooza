package com.example.pedalpalooza.data

interface EventRepository {
    suspend fun searchEvents(
        query: String,
        limit: Int,
        offset: Int
    ): ShiftResponse
}

class ShiftEventRepository(
    private val apiService: ShiftApiService = ShiftApiService.instance
) : EventRepository {

    override suspend fun searchEvents(
        query: String,
        limit: Int,
        offset: Int
    ): ShiftResponse {
        val normalizedQuery = query.trim().ifEmpty { "%" }
        return apiService.searchEvents(
            query = normalizedQuery,
            limit = limit,
            offset = offset
        )
    }
}
