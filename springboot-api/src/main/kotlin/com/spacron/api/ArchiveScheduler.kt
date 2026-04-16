package com.spacron.api

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ArchiveScheduler(
    private val userActivityArchiveService: UserActivityArchiveService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 0 2 * * *")
    fun runDailyArchive() {
        logger.info("Starting daily user activity archive job")
        try {
            val archivedCount = userActivityArchiveService.archiveOldActivities()
            logger.info("Daily archive job completed successfully: {} records archived", archivedCount)
        } catch (exception: Exception) {
            logger.error("Failed to complete daily archive job", exception)
        }
    }
}
