package com.financemanager.service

import com.financemanager.entity.Category
import com.financemanager.entity.CategoryType
import com.financemanager.entity.Transaction
import com.financemanager.entity.User
import com.financemanager.exception.BadRequestException
import com.financemanager.repository.TransactionRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.math.BigDecimal
import java.time.LocalDate

class ReportServiceTest {

    private lateinit var transactionRepository: TransactionRepository
    private lateinit var reportService: ReportService

    private val testUser = User(id = 1L, username = "test@example.com", password = "hashed", fullName = "Test", phoneNumber = "123")
    private val salaryCategory = Category(id = 1L, name = "Salary", type = CategoryType.INCOME, isDefault = true)
    private val freelanceCategory = Category(id = 2L, name = "Freelance", type = CategoryType.INCOME, isDefault = false, user = testUser)
    private val foodCategory = Category(id = 3L, name = "Food", type = CategoryType.EXPENSE, isDefault = true)
    private val rentCategory = Category(id = 4L, name = "Rent", type = CategoryType.EXPENSE, isDefault = true)

    @BeforeEach
    fun setUp() {
        transactionRepository = mock()
        reportService = ReportService(transactionRepository)
    }

    // ── Monthly Report ──────────────────────────────────────────────────

    @Test
    fun `getMonthlyReport - aggregates income and expenses correctly`() {
        val transactions = listOf(
            Transaction(id = 1L, amount = BigDecimal("3000.00"), date = LocalDate.of(2024, 3, 5), category = salaryCategory, user = testUser),
            Transaction(id = 2L, amount = BigDecimal("500.00"), date = LocalDate.of(2024, 3, 10), category = freelanceCategory, user = testUser),
            Transaction(id = 3L, amount = BigDecimal("200.00"), date = LocalDate.of(2024, 3, 12), category = foodCategory, user = testUser),
            Transaction(id = 4L, amount = BigDecimal("800.00"), date = LocalDate.of(2024, 3, 15), category = rentCategory, user = testUser)
        )
        whenever(transactionRepository.findByUserAndYearAndMonth(testUser, 2024, 3)).thenReturn(transactions)

        val result = reportService.getMonthlyReport(testUser, 2024, 3)

        assertEquals(2024, result.year)
        assertEquals(3, result.month)
        assertEquals(BigDecimal("3500.00"), result.totalIncome)
        assertEquals(BigDecimal("1000.00"), result.totalExpenses)
        assertEquals(BigDecimal("2500.00"), result.netSavings)
        assertEquals(2, result.incomeByCategory.size)
        assertEquals(2, result.expensesByCategory.size)
    }

    @Test
    fun `getMonthlyReport - returns zeros when no transactions`() {
        whenever(transactionRepository.findByUserAndYearAndMonth(testUser, 2024, 1)).thenReturn(emptyList())

        val result = reportService.getMonthlyReport(testUser, 2024, 1)

        assertEquals(BigDecimal("0.00"), result.totalIncome)
        assertEquals(BigDecimal("0.00"), result.totalExpenses)
        assertEquals(BigDecimal("0.00"), result.netSavings)
        assertTrue(result.incomeByCategory.isEmpty())
        assertTrue(result.expensesByCategory.isEmpty())
    }

    @Test
    fun `getMonthlyReport - income by category sorted by total descending`() {
        val transactions = listOf(
            Transaction(id = 1L, amount = BigDecimal("3000.00"), date = LocalDate.of(2024, 3, 1), category = salaryCategory, user = testUser),
            Transaction(id = 2L, amount = BigDecimal("500.00"), date = LocalDate.of(2024, 3, 2), category = freelanceCategory, user = testUser)
        )
        whenever(transactionRepository.findByUserAndYearAndMonth(testUser, 2024, 3)).thenReturn(transactions)

        val result = reportService.getMonthlyReport(testUser, 2024, 3)

        assertEquals("Salary", result.incomeByCategory[0].categoryName)
        assertEquals("Freelance", result.incomeByCategory[1].categoryName)
    }

    @Test
    fun `getMonthlyReport - net savings is negative when expenses exceed income`() {
        val transactions = listOf(
            Transaction(id = 1L, amount = BigDecimal("500.00"), date = LocalDate.of(2024, 3, 1), category = salaryCategory, user = testUser),
            Transaction(id = 2L, amount = BigDecimal("1500.00"), date = LocalDate.of(2024, 3, 5), category = foodCategory, user = testUser)
        )
        whenever(transactionRepository.findByUserAndYearAndMonth(testUser, 2024, 3)).thenReturn(transactions)

        val result = reportService.getMonthlyReport(testUser, 2024, 3)

        assertEquals(BigDecimal("-1000.00"), result.netSavings)
    }

    @Test
    fun `getMonthlyReport - throws BadRequestException for invalid month`() {
        assertThrows<BadRequestException> { reportService.getMonthlyReport(testUser, 2024, 0) }
        assertThrows<BadRequestException> { reportService.getMonthlyReport(testUser, 2024, 13) }
    }

    @Test
    fun `getMonthlyReport - throws BadRequestException for invalid year`() {
        assertThrows<BadRequestException> { reportService.getMonthlyReport(testUser, 1999, 1) }
        assertThrows<BadRequestException> { reportService.getMonthlyReport(testUser, 2101, 1) }
    }

    // ── Yearly Report ───────────────────────────────────────────────────

    @Test
    fun `getYearlyReport - aggregates entire year correctly`() {
        val transactions = listOf(
            Transaction(id = 1L, amount = BigDecimal("3000.00"), date = LocalDate.of(2024, 1, 5), category = salaryCategory, user = testUser),
            Transaction(id = 2L, amount = BigDecimal("3000.00"), date = LocalDate.of(2024, 2, 5), category = salaryCategory, user = testUser),
            Transaction(id = 3L, amount = BigDecimal("200.00"), date = LocalDate.of(2024, 1, 10), category = foodCategory, user = testUser),
            Transaction(id = 4L, amount = BigDecimal("300.00"), date = LocalDate.of(2024, 2, 10), category = foodCategory, user = testUser)
        )
        whenever(transactionRepository.findByUserAndYear(testUser, 2024)).thenReturn(transactions)

        val result = reportService.getYearlyReport(testUser, 2024)

        assertEquals(2024, result.year)
        assertEquals(BigDecimal("6000.00"), result.totalIncome)
        assertEquals(BigDecimal("500.00"), result.totalExpenses)
        assertEquals(BigDecimal("5500.00"), result.netSavings)
    }

    @Test
    fun `getYearlyReport - monthly breakdown has 12 months`() {
        whenever(transactionRepository.findByUserAndYear(testUser, 2024)).thenReturn(emptyList())

        val result = reportService.getYearlyReport(testUser, 2024)

        assertEquals(12, result.monthlyBreakdown.size)
        assertEquals(1, result.monthlyBreakdown[0].month)
        assertEquals(12, result.monthlyBreakdown[11].month)
    }

    @Test
    fun `getYearlyReport - monthly breakdown correctly splits by month`() {
        val transactions = listOf(
            Transaction(id = 1L, amount = BigDecimal("1000.00"), date = LocalDate.of(2024, 1, 15), category = salaryCategory, user = testUser),
            Transaction(id = 2L, amount = BigDecimal("2000.00"), date = LocalDate.of(2024, 6, 15), category = salaryCategory, user = testUser),
            Transaction(id = 3L, amount = BigDecimal("400.00"), date = LocalDate.of(2024, 1, 20), category = foodCategory, user = testUser)
        )
        whenever(transactionRepository.findByUserAndYear(testUser, 2024)).thenReturn(transactions)

        val result = reportService.getYearlyReport(testUser, 2024)

        val janBreakdown = result.monthlyBreakdown.first { it.month == 1 }
        val junBreakdown = result.monthlyBreakdown.first { it.month == 6 }
        val febBreakdown = result.monthlyBreakdown.first { it.month == 2 }

        assertEquals(BigDecimal("1000.00"), janBreakdown.totalIncome)
        assertEquals(BigDecimal("400.00"), janBreakdown.totalExpenses)
        assertEquals(BigDecimal("2000.00"), junBreakdown.totalIncome)
        assertEquals(BigDecimal("0.00"), febBreakdown.totalIncome)
        assertEquals(BigDecimal("0.00"), febBreakdown.totalExpenses)
    }

    @Test
    fun `getYearlyReport - throws BadRequestException for invalid year`() {
        assertThrows<BadRequestException> { reportService.getYearlyReport(testUser, 1999) }
        assertThrows<BadRequestException> { reportService.getYearlyReport(testUser, 2101) }
    }

    @Test
    fun `getYearlyReport - categories grouped and sorted correctly`() {
        val transactions = listOf(
            Transaction(id = 1L, amount = BigDecimal("1000.00"), date = LocalDate.of(2024, 1, 1), category = freelanceCategory, user = testUser),
            Transaction(id = 2L, amount = BigDecimal("5000.00"), date = LocalDate.of(2024, 2, 1), category = salaryCategory, user = testUser),
            Transaction(id = 3L, amount = BigDecimal("300.00"), date = LocalDate.of(2024, 3, 1), category = foodCategory, user = testUser),
            Transaction(id = 4L, amount = BigDecimal("800.00"), date = LocalDate.of(2024, 4, 1), category = rentCategory, user = testUser)
        )
        whenever(transactionRepository.findByUserAndYear(testUser, 2024)).thenReturn(transactions)

        val result = reportService.getYearlyReport(testUser, 2024)

        // Salary (5000) should be first income category
        assertEquals("Salary", result.incomeByCategory[0].categoryName)
        assertEquals("Freelance", result.incomeByCategory[1].categoryName)
        // Rent (800) should be first expense category
        assertEquals("Rent", result.expensesByCategory[0].categoryName)
        assertEquals("Food", result.expensesByCategory[1].categoryName)
    }
}
