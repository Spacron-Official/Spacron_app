package com.spacron.api

import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/archives")
class ArchiveController(
    private val archiveStorage: ArchiveStorage,
) {
    @GetMapping
    fun listArchiveFiles(): ResponseEntity<List<String>> {
        val archiveFiles = archiveStorage.listArchiveFiles()
        return ResponseEntity.ok(archiveFiles)
    }

    @GetMapping("/{fileName}")
    fun downloadArchiveFile(@PathVariable fileName: String): ResponseEntity<Resource> {
        val resource = archiveStorage.loadArchive(fileName)
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$fileName\"")
            .contentType(MediaType.APPLICATION_JSON)
            .body(resource)
    }
}
