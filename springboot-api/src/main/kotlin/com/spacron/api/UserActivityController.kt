package com.spacron.api

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/user-activities")
class UserActivityController(
    private val userActivityService: UserActivityService,
) {
    @PostMapping
    fun create(@Valid @RequestBody request: UserActivityRequest): ResponseEntity<UserActivityResponse> {
        val activity = userActivityService.create(request)
        return ResponseEntity.ok(activity.toResponse())
    }

    @GetMapping
    fun list(): ResponseEntity<List<UserActivityResponse>> {
        val activities = userActivityService.list().map(UserActivity::toResponse)
        return ResponseEntity.ok(activities)
    }

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): ResponseEntity<UserActivityResponse> {
        val activity = userActivityService.findById(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(activity.toResponse())
    }

    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @Valid @RequestBody request: UserActivityRequest): ResponseEntity<UserActivityResponse> {
        val activity = userActivityService.update(id, request) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(activity.toResponse())
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        userActivityService.delete(id)
        return ResponseEntity.noContent().build()
    }
}
