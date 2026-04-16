package com.spacron.api

import java.time.Instant

data class UserActivityResponse(
    val id: Long,
    val userId: Long,
    val action: String,
    val createdAt: Instant,
)
