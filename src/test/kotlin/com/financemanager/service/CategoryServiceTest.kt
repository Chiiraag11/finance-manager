package com.financemanager.service

import com.financemanager.dto.CreateCategoryRequest
import com.financemanager.entity.Category
import com.financemanager.entity.CategoryType
import com.financemanager.entity.User
import com.financemanager.exception.BadRequestException
import com.financemanager.exception.ConflictException
import com.financemanager.exception.DuplicateResourceException
import com.financemanager.exception.ResourceNotFoundException
import com.financemanager.repository.CategoryRepository
import com.financemanager.repository.TransactionRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.util.Optional

class CategoryServiceTest {

    private lateinit var categoryRepository: CategoryRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var categoryService: CategoryService

    private val testUser = User(id = 1L, username = "test@example.com", password = "hashed", fullName = "Test", phoneNumber = "1234567890")

    @BeforeEach
    fun setUp() {
        categoryRepository = mock()
        transactionRepository = mock()
        categoryService = CategoryService(categoryRepository, transactionRepository)
    }

    @Test
    fun `getCategories - returns all default and user categories`() {
        val defaultCat = Category(id = 1L, name = "Salary", type = CategoryType.INCOME, isDefault = true)
        val userCat = Category(id = 2L, name = "Freelance", type = CategoryType.INCOME, isDefault = false, user = testUser)
        whenever(categoryRepository.findAllForUser(testUser)).thenReturn(listOf(defaultCat, userCat))

        val result = categoryService.getCategories(testUser)

        assertEquals(2, result.size)
        assertTrue(result.any { it.name == "Salary" && it.isDefault })
        assertTrue(result.any { it.name == "Freelance" && !it.isDefault })
    }

    @Test
    fun `createCategory - success creates new category`() {
        val request = CreateCategoryRequest(name = "Freelance", type = CategoryType.INCOME)
        val saved = Category(id = 10L, name = "Freelance", type = CategoryType.INCOME, user = testUser)

        whenever(categoryRepository.existsByNameIgnoreCaseForUser("Freelance", testUser)).thenReturn(false)
        whenever(categoryRepository.save(any())).thenReturn(saved)

        val result = categoryService.createCategory(testUser, request)

        assertEquals("Freelance", result.name)
        assertEquals(CategoryType.INCOME, result.type)
        assertFalse(result.isDefault)
    }

    @Test
    fun `createCategory - throws DuplicateResourceException when name exists`() {
        val request = CreateCategoryRequest(name = "Salary", type = CategoryType.INCOME)
        whenever(categoryRepository.existsByNameIgnoreCaseForUser("Salary", testUser)).thenReturn(true)

        assertThrows<DuplicateResourceException> { categoryService.createCategory(testUser, request) }
        verify(categoryRepository, never()).save(any())
    }

    @Test
    fun `createCategory - trims whitespace from name`() {
        val request = CreateCategoryRequest(name = "  Freelance  ", type = CategoryType.INCOME)
        val saved = Category(id = 10L, name = "Freelance", type = CategoryType.INCOME, user = testUser)

        whenever(categoryRepository.existsByNameIgnoreCaseForUser("Freelance", testUser)).thenReturn(false)
        whenever(categoryRepository.save(any())).thenReturn(saved)

        categoryService.createCategory(testUser, request)

        verify(categoryRepository).existsByNameIgnoreCaseForUser("Freelance", testUser)
    }

    @Test
    fun `deleteCategory - success deletes user category`() {
        val category = Category(id = 5L, name = "Freelance", type = CategoryType.INCOME, user = testUser)

        whenever(categoryRepository.findByNameAndUserIsNull("Freelance")).thenReturn(Optional.empty())
        whenever(categoryRepository.findByNameAndUser("Freelance", testUser)).thenReturn(Optional.of(category))
        whenever(transactionRepository.existsByCategoryAndDeletedFalse(category)).thenReturn(false)

        categoryService.deleteCategory(testUser, "Freelance")

        verify(categoryRepository).delete(category)
    }

    @Test
    fun `deleteCategory - throws BadRequestException for default category`() {
        val defaultCat = Category(id = 1L, name = "Salary", type = CategoryType.INCOME, isDefault = true)
        whenever(categoryRepository.findByNameAndUserIsNull("Salary")).thenReturn(Optional.of(defaultCat))

        assertThrows<BadRequestException> { categoryService.deleteCategory(testUser, "Salary") }
        verify(categoryRepository, never()).delete(any())
    }

    @Test
    fun `deleteCategory - throws ResourceNotFoundException when category not found`() {
        whenever(categoryRepository.findByNameAndUserIsNull("Unknown")).thenReturn(Optional.empty())
        whenever(categoryRepository.findByNameAndUser("Unknown", testUser)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> { categoryService.deleteCategory(testUser, "Unknown") }
    }

    @Test
    fun `deleteCategory - throws ConflictException when category has transactions`() {
        val category = Category(id = 5L, name = "Freelance", type = CategoryType.INCOME, user = testUser)

        whenever(categoryRepository.findByNameAndUserIsNull("Freelance")).thenReturn(Optional.empty())
        whenever(categoryRepository.findByNameAndUser("Freelance", testUser)).thenReturn(Optional.of(category))
        whenever(transactionRepository.existsByCategoryAndDeletedFalse(category)).thenReturn(true)

        assertThrows<ConflictException> { categoryService.deleteCategory(testUser, "Freelance") }
        verify(categoryRepository, never()).delete(any())
    }

    @Test
    fun `findCategoryByIdForUser - returns category when found`() {
        val category = Category(id = 1L, name = "Salary", type = CategoryType.INCOME, isDefault = true)
        whenever(categoryRepository.findByIdAndAccessibleByUser(1L, testUser)).thenReturn(Optional.of(category))

        val result = categoryService.findCategoryByIdForUser(1L, testUser)
        assertEquals("Salary", result.name)
    }

    @Test
    fun `findCategoryByIdForUser - throws ResourceNotFoundException when not found`() {
        whenever(categoryRepository.findByIdAndAccessibleByUser(99L, testUser)).thenReturn(Optional.empty())
        assertThrows<ResourceNotFoundException> { categoryService.findCategoryByIdForUser(99L, testUser) }
    }
}
