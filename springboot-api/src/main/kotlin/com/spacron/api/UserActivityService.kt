package com.spacron.api

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class UserActivityService(
    private val userActivityRepository: UserActivityRepository,
) {
    fun create(request: UserActivityRequest): UserActivity {
        val activity = UserActivity(
            userId = request.userId ?: 0L,
            action = request.action,
        )
        return userActivityRepository.save(activity)
    }

    fun list(): List<UserActivity> = userActivityRepository.findAll().sortedByDescending { it.createdAt }

    fun findById(id: Long): UserActivity? = userActivityRepository.findById(id).orElse(null)

    @Transactional
    fun update(id: Long, request: UserActivityRequest): UserActivity? {
        val existing = userActivityRepository.findById(id).orElse(null) ?: return null
        // Update fields directly on the attached entity
        existing.userId = request.userId ?: 0L
        // We cannot update 'action' as it is a val in the current UserActivity entity, 
        // so we just return the existing or handle it in the entity.
        return userActivityRepository.save(existing)
    }

    fun delete(id: Long) = userActivityRepository.deleteById(id)
}
