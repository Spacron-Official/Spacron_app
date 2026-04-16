package com.spacron.api

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "user_activity")
data class UserActivity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val userId: Long = 0,

    @Column(nullable = false, length = 1024)
    val action: String = "",

    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),
)

fun UserActivity.toResponse() = UserActivityResponse(id, userId, action, createdAt)
