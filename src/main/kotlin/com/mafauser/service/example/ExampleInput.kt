package com.mafauser.service.example

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateExampleInput(
    @field:NotBlank @field:Size(max = 255) val name: String,
    @field:Size(max = 1024) val description: String? = null,
)

data class UpdateExampleInput(
    @field:Size(min = 1, max = 255) val name: String? = null,
    @field:Size(max = 1024) val description: String? = null,
    val clearDescription: Boolean = false,
)
