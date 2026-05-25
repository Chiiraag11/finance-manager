package com.financemanager.dto

import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.time.LocalDate

data class CreateGoalRequest(
    @field:NotBlank(message = "Goal name is required")
    @field:Size(max = 200, message = "Goal name must not exceed 200 characters")
    val goalName: String,

    @field:NotNull(message = "Target amount is required")
    @field:DecimalMin(value = "0.01", message = "Target amount must be greater than 0")
    @field:Digits(integer = 17, fraction = 2, message = "Target amount must have at most 2 decimal places")
    val targetAmount: BigDecimal,

    @field:NotNull(message = "Target date is required")
    val targetDate: LocalDate,

    val startDate: LocalDate? = null
)

data class UpdateGoalRequest(
    @field:NotBlank(message = "Goal name is required")
    @field:Size(max = 200, message = "Goal name must not exceed 200 characters")
    val goalName: String,

    @field:NotNull(message = "Target amount is required")
    @field:DecimalMin(value = "0.01", message = "Target amount must be greater than 0")
    @field:Digits(integer = 17, fraction = 2, message = "Target amount must have at most 2 decimal places")
    val targetAmount: BigDecimal,

    @field:NotNull(message = "Target date is required")
    val targetDate: LocalDate
)

data class GoalResponse(
    val id: Long,
    val goalName: String,
    val targetAmount: BigDecimal,
    val targetDate: LocalDate,
    val startDate: LocalDate,
    val currentProgress: BigDecimal,
    val progressPercentage: BigDecimal,
    val remainingAmount: BigDecimal
)
