package com.spacron.api

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "user_activity")
data class UserActivity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var userId: Long = 0,

    @Column(nullable = false, length = 1024)
    val action: String = "",

    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),
)

fun UserActivity.toResponse() = UserActivityResponse(id ?: 0L, userId, action, createdAt)
