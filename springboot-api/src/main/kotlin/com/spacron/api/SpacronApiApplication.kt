package com.spacron.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableConfigurationProperties(SpacronProperties::class)
@EnableScheduling
class SpacronApiApplication

@ConfigurationProperties(prefix = "spacron")
data class SpacronProperties(
    var adminEmail: String = "officialspacron1426@gmail.com",
    var adminPassword: String = "change-me",
    var adminName: String = "Spacron Official",
    var defaultCommissionPct: Int = 15,
    var sessionDays: Long = 7,
)

fun main(args: Array<String>) {
    runApplication<SpacronApiApplication>(*args)
}
