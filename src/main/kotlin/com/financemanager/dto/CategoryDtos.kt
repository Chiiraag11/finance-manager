package com.financemanager.dto

import com.financemanager.entity.CategoryType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class CreateCategoryRequest(
    @field:NotBlank(message = "Category name is required")
    @field:Size(max = 100, message = "Category name must not exceed 100 characters")
    val name: String,

    @field:NotNull(message = "Category type is required (INCOME or EXPENSE)")
    val type: CategoryType
)

data class CategoryResponse(
    val id: Long,
    val name: String,
    val type: CategoryType,
    val isDefault: Boolean
)
