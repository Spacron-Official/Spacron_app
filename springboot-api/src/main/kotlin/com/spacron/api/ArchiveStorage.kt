package com.spacron.api

import org.springframework.core.io.Resource
import java.nio.file.Path
import java.time.LocalDate

interface ArchiveStorage {
    fun storeArchive(date: LocalDate, payload: String): Path
    fun listArchiveFiles(): List<String>
    fun loadArchive(fileName: String): Resource
}
