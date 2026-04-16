package com.spacron.api

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class UserActivityRequest(
    @field:NotNull
    val userId: Long? = null,

    @field:NotBlank
    val action: String = "",
)
