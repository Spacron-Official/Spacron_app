package com.spacron.api

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Service
class UserActivityArchiveService(
    private val userActivityRepository: UserActivityRepository,
    private val archiveStorage: ArchiveStorage,
    private val objectMapper: ObjectMapper,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun archiveOldActivities(): Int {
        val cutoff = Instant.now().minus(30, ChronoUnit.DAYS)
        val oldActivities = userActivityRepository.findByCreatedAtBefore(cutoff)
        if (oldActivities.isEmpty()) {
            logger.info("No user activity older than 30 days found for archiving.")
            return 0
        }

        val archivePayload = objectMapper.writerWithDefaultPrettyPrinter()
            .writeValueAsString(oldActivities)

        val archivePath = archiveStorage.storeArchive(LocalDate.now(), archivePayload)
        userActivityRepository.deleteAll(oldActivities)

        logger.info(
            "Archived {} user activity records to file {} and deleted original rows.",
            oldActivities.size,
            archivePath.fileName,
        )
        return oldActivities.size
    }
}
