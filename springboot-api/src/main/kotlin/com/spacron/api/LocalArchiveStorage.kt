package com.spacron.api

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Component
class LocalArchiveStorage(
    @Value("\${archive.directory:archive}")
    private val archiveDirectory: String,
) : ArchiveStorage {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val archiveRoot: Path = Paths.get(archiveDirectory).toAbsolutePath()

    init {
        Files.createDirectories(archiveRoot)
        logger.info("Archive storage initialized at {}", archiveRoot)
    }

    override fun storeArchive(date: LocalDate, payload: String): Path {
        val fileName = "archive_${date.format(DateTimeFormatter.ISO_DATE)}.json"
        val targetPath = archiveRoot.resolve(fileName)
        Files.writeString(targetPath, payload, StandardCharsets.UTF_8)
        logger.info("Saved archive file {}", targetPath)
        return targetPath
    }

    override fun listArchiveFiles(): List<String> =
        Files.list(archiveRoot).use { stream ->
            stream
                .filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".json") }
                .map { it.fileName.toString() }
                .sorted()
                .toList()
        }

    override fun loadArchive(fileName: String): Resource {
        val normalized = archiveRoot.resolve(fileName).normalize()
        if (!normalized.startsWith(archiveRoot) || !Files.exists(normalized)) {
            throw IllegalArgumentException("Archive file not found: $fileName")
        }
        return UrlResource(normalized.toUri())
    }
}
