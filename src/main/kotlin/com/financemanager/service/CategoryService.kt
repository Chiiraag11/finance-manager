package com.financemanager.service

import com.financemanager.dto.CategoryResponse
import com.financemanager.dto.CreateCategoryRequest
import com.financemanager.entity.Category
import com.financemanager.entity.User
import com.financemanager.exception.BadRequestException
import com.financemanager.exception.ConflictException
import com.financemanager.exception.DuplicateResourceException
import com.financemanager.exception.ResourceNotFoundException
import com.financemanager.repository.CategoryRepository
import com.financemanager.repository.TransactionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CategoryService(
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository
) {

    @Transactional(readOnly = true)
    fun getCategories(user: User): List<CategoryResponse> =
        categoryRepository.findAllForUser(user).map { it.toResponse() }

    @Transactional
    fun createCategory(user: User, request: CreateCategoryRequest): CategoryResponse {

        val trimmedName = request.name.trim()

        if (categoryRepository.existsByNameIgnoreCaseForUser(trimmedName, user)) {
            throw DuplicateResourceException(
                "A category named '$trimmedName' already exists"
            )
        }

        val category = Category(
            name = trimmedName,
            type = request.type,
            isDefault = false,
            user = user
        )

        return categoryRepository.save(category).toResponse()
    }

    @Transactional
    fun deleteCategory(user: User, name: String): Unit {

        val trimmedName = name.trim()

        val defaultCat = categoryRepository.findByNameAndUserIsNull(trimmedName)

        if (defaultCat.isPresent) {
            throw BadRequestException(
                "Default category '$trimmedName' cannot be deleted"
            )
        }

        val category = categoryRepository.findByNameAndUser(
            trimmedName,
            user
        ).orElseThrow {
            ResourceNotFoundException(
                "Category '$trimmedName' not found"
            )
        }

        if (transactionRepository.existsByCategoryAndDeletedFalse(category)) {
            throw ConflictException(
                "Category '$trimmedName' cannot be deleted because it has associated transactions"
            )
        }

        categoryRepository.delete(category)
    }

    @Transactional(readOnly = true)
    fun findCategoryByIdForUser(id: Long, user: User): Category =
        categoryRepository.findByIdAndAccessibleByUser(id, user)
            .orElseThrow {
                ResourceNotFoundException(
                    "Category with id $id not found"
                )
            }

    @Transactional(readOnly = true)
    fun findCategoryByNameForUser(name: String, user: User): Category {

        val trimmedName = name.trim()

        return categoryRepository.findAllForUser(user)
            .firstOrNull {
                it.name.equals(trimmedName, ignoreCase = true)
            }
            ?: throw ResourceNotFoundException(
                "Category with name $trimmedName not found"
            )
    }

    private fun Category.toResponse() = CategoryResponse(
        id = id,
        name = name,
        type = type,
        isDefault = isDefault
    )
}