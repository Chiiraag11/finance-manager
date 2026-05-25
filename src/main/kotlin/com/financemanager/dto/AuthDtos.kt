package com.financemanager.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class RegisterRequest(
    @field:NotBlank(message = "Username (email) is required")
    @field:Email(message = "Must be a valid email address")
    val username: String,

    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, message = "Password must be at least 8 characters")
    val password: String,

    @field:NotBlank(message = "Full name is required")
    @field:Size(max = 100, message = "Full name must not exceed 100 characters")
    val fullName: String,

    @field:NotBlank(message = "Phone number is required")
    @field:Pattern(
        regexp = "^[+]?[0-9]{7,15}$",
        message = "Phone number must be 7-15 digits, optionally prefixed with +"
    )
    val phoneNumber: String
)

data class LoginRequest(
    @field:NotBlank(message = "Username is required")
    val username: String,

    @field:NotBlank(message = "Password is required")
    val password: String
)

data class UserResponse(
    val id: Long,
    val username: String,
    val fullName: String,
    val phoneNumber: String
)
