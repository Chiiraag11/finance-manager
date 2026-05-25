package com.financemanager.service

import com.financemanager.dto.CreateTransactionRequest
import com.financemanager.dto.TransactionFilterRequest
import com.financemanager.dto.UpdateTransactionRequest
import com.financemanager.entity.Category
import com.financemanager.entity.CategoryType
import com.financemanager.entity.Transaction
import com.financemanager.entity.User
import com.financemanager.exception.BadRequestException
import com.financemanager.exception.ResourceNotFoundException
import com.financemanager.repository.TransactionRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional

class TransactionServiceTest {

    private lateinit var transactionRepository: TransactionRepository
    private lateinit var categoryService: CategoryService
    private lateinit var transactionService: TransactionService

    private val testUser = User(id = 1L, username = "test@example.com", password = "hashed", fullName = "Test", phoneNumber = "123")
    private val salaryCategory = Category(id = 1L, name = "Salary", type = CategoryType.INCOME, isDefault = true)
    private val foodCategory = Category(id = 2L, name = "Food", type = CategoryType.EXPENSE, isDefault = true)

    @BeforeEach
    fun setUp() {
        transactionRepository = mock()
        categoryService = mock()
        transactionService = TransactionService(transactionRepository, categoryService)
    }

    @Test
    fun `createTransaction - success creates transaction`() {
        val request = CreateTransactionRequest(
            amount = BigDecimal("1500.00"),
            date = LocalDate.now(),
            categoryId = 1L,
            description = "Monthly salary"
        )
        val saved = Transaction(id = 1L, amount = BigDecimal("1500.00"), date = LocalDate.now(), category = salaryCategory, user = testUser)

        whenever(categoryService.findCategoryByIdForUser(1L, testUser)).thenReturn(salaryCategory)
        whenever(transactionRepository.save(any())).thenReturn(saved)

        val result = transactionService.createTransaction(testUser, request)

        assertEquals(BigDecimal("1500.00"), result.amount)
        assertEquals("Salary", result.categoryName)
    }

    @Test
    fun `createTransaction - throws BadRequestException for future date`() {
        val request = CreateTransactionRequest(
            amount = BigDecimal("100.00"),
            date = LocalDate.now().plusDays(1),
            categoryId = 1L
        )

        assertThrows<BadRequestException> { transactionService.createTransaction(testUser, request) }
        verify(transactionRepository, never()).save(any())
    }

    @Test
    fun `createTransaction - today's date is valid`() {
        val request = CreateTransactionRequest(
            amount = BigDecimal("100.00"),
            date = LocalDate.now(),
            categoryId = 2L
        )
        val saved = Transaction(id = 1L, amount = BigDecimal("100.00"), date = LocalDate.now(), category = foodCategory, user = testUser)

        whenever(categoryService.findCategoryByIdForUser(2L, testUser)).thenReturn(foodCategory)
        whenever(transactionRepository.save(any())).thenReturn(saved)

        assertDoesNotThrow { transactionService.createTransaction(testUser, request) }
    }

    @Test
    fun `getTransactions - returns filtered transactions`() {
        val tx1 = Transaction(id = 1L, amount = BigDecimal("100"), date = LocalDate.now(), category = foodCategory, user = testUser)
        val tx2 = Transaction(id = 2L, amount = BigDecimal("1500"), date = LocalDate.now().minusDays(1), category = salaryCategory, user = testUser)

        whenever(transactionRepository.findByUserWithFilters(testUser, null, null, null))
            .thenReturn(listOf(tx1, tx2))

        val result = transactionService.getTransactions(testUser, TransactionFilterRequest())

        assertEquals(2, result.size)
    }

    @Test
    fun `getTransactions - throws BadRequestException when startDate after endDate`() {
        val filters = TransactionFilterRequest(
            startDate = LocalDate.now(),
            endDate = LocalDate.now().minusDays(1)
        )

        assertThrows<BadRequestException> { transactionService.getTransactions(testUser, filters) }
    }

    @Test
    fun `getTransactions - passes filters to repository`() {
        val start = LocalDate.of(2024, 1, 1)
        val end = LocalDate.of(2024, 1, 31)
        val filters = TransactionFilterRequest(startDate = start, endDate = end, categoryId = 1L)

        whenever(transactionRepository.findByUserWithFilters(testUser, start, end, 1L)).thenReturn(emptyList())

        transactionService.getTransactions(testUser, filters)

        verify(transactionRepository).findByUserWithFilters(testUser, start, end, 1L)
    }

    @Test
    fun `updateTransaction - success updates amount and category`() {
        val existing = Transaction(id = 1L, amount = BigDecimal("100"), date = LocalDate.now(), category = foodCategory, user = testUser)
        val request = UpdateTransactionRequest(amount = BigDecimal("200"), categoryId = 1L, description = "Updated")

        whenever(transactionRepository.findByIdAndUserAndDeletedFalse(1L, testUser)).thenReturn(Optional.of(existing))
        whenever(categoryService.findCategoryByIdForUser(1L, testUser)).thenReturn(salaryCategory)
        whenever(transactionRepository.save(any())).thenReturn(existing)

        val result = transactionService.updateTransaction(testUser, 1L, request)

        assertEquals(BigDecimal("200"), existing.amount)
        assertEquals(salaryCategory, existing.category)
    }

    @Test
    fun `updateTransaction - throws ResourceNotFoundException when transaction not found`() {
        val request = UpdateTransactionRequest(amount = BigDecimal("100"), categoryId = 1L)
        whenever(transactionRepository.findByIdAndUserAndDeletedFalse(99L, testUser)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> { transactionService.updateTransaction(testUser, 99L, request) }
    }

    @Test
    fun `deleteTransaction - success soft deletes transaction`() {
        val tx = Transaction(id = 1L, amount = BigDecimal("100"), date = LocalDate.now(), category = foodCategory, user = testUser)
        whenever(transactionRepository.findByIdAndUserAndDeletedFalse(1L, testUser)).thenReturn(Optional.of(tx))
        whenever(transactionRepository.save(any())).thenReturn(tx)

        transactionService.deleteTransaction(testUser, 1L)

        assertTrue(tx.deleted)
        verify(transactionRepository).save(tx)
    }

    @Test
    fun `deleteTransaction - throws ResourceNotFoundException when not found`() {
        whenever(transactionRepository.findByIdAndUserAndDeletedFalse(99L, testUser)).thenReturn(Optional.empty())
        assertThrows<ResourceNotFoundException> { transactionService.deleteTransaction(testUser, 99L) }
    }
}
