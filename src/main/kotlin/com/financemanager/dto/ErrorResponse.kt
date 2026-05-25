package com.financemanager.dto

import java.time.LocalDateTime

data class ErrorResponse(
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val status: Int,
    val error: String,
    val message: String,
    val path: String? = null
)

data class ValidationErrorResponse(
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val status: Int = 400,
    val error: String = "Validation Failed",
    val message: String = "One or more fields are invalid",
    val fieldErrors: Map<String, String>
)

data class MessageResponse(
    val message: String
)
