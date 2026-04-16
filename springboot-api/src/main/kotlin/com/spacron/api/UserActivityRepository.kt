package com.spacron.api

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface UserActivityRepository : JpaRepository<UserActivity, Long> {
    fun findByCreatedAtBefore(cutoff: Instant): List<UserActivity>
}
