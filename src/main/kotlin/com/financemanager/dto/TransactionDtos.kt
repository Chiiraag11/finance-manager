package com.financemanager.dto

import com.financemanager.entity.CategoryType
import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.time.LocalDate

data class CreateTransactionRequest(

    @field:NotNull(message = "Amount is required")
    @field:DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @field:Digits(integer = 17, fraction = 2, message = "Amount must have at most 2 decimal places")
    val amount: BigDecimal,

    @field:NotNull(message = "Date is required")
    val date: LocalDate,

    @field:NotBlank(message = "Category is required")
    val category: String,

    @field:Size(max = 500, message = "Description must not exceed 500 characters")
    val description: String? = null
)

data class UpdateTransactionRequest(

    @field:NotNull(message = "Amount is required")
    @field:DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @field:Digits(integer = 17, fraction = 2, message = "Amount must have at most 2 decimal places")
    val amount: BigDecimal,

    @field:NotBlank(message = "Category is required")
    val category: String,

    @field:Size(max = 500, message = "Description must not exceed 500 characters")
    val description: String? = null
)

data class TransactionResponse(
    val id: Long,
    val amount: BigDecimal,
    val date: LocalDate,
    val categoryId: Long,
    val categoryName: String,
    val categoryType: CategoryType,
    val description: String?
)

data class TransactionFilterRequest(
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val categoryId: Long? = null
)