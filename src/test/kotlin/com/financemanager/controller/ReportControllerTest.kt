package com.financemanager.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.financemanager.config.SecurityConfig
import com.financemanager.dto.CategorySummary
import com.financemanager.dto.MonthlyBreakdown
import com.financemanager.dto.MonthlyReportResponse
import com.financemanager.dto.YearlyReportResponse
import com.financemanager.entity.User
import com.financemanager.exception.BadRequestException
import com.financemanager.exception.GlobalExceptionHandler
import com.financemanager.security.UserDetailsServiceImpl
import com.financemanager.service.AuthService
import com.financemanager.service.ReportService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.math.BigDecimal

@WebMvcTest(ReportController::class)
@Import(SecurityConfig::class, GlobalExceptionHandler::class)
class ReportControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var reportService: ReportService

    @MockBean
    private lateinit var authService: AuthService

    @MockBean
    private lateinit var userDetailsService: UserDetailsServiceImpl

    private val testUser = User(id = 1L, username = "test@example.com", password = "hashed", fullName = "Test", phoneNumber = "123")

    private val sampleMonthlyReport = MonthlyReportResponse(
        year = 2024,
        month = 3,
        totalIncome = BigDecimal("3500.00"),
        totalExpenses = BigDecimal("1000.00"),
        netSavings = BigDecimal("2500.00"),
        incomeByCategory = listOf(CategorySummary(1L, "Salary", BigDecimal("3000.00"))),
        expensesByCategory = listOf(CategorySummary(3L, "Food", BigDecimal("1000.00")))
    )

    private val sampleYearlyReport = YearlyReportResponse(
        year = 2024,
        totalIncome = BigDecimal("42000.00"),
        totalExpenses = BigDecimal("12000.00"),
        netSavings = BigDecimal("30000.00"),
        monthlyBreakdown = (1..12).map { MonthlyBreakdown(it, BigDecimal("3500.00"), BigDecimal("1000.00"), BigDecimal("2500.00")) },
        incomeByCategory = listOf(CategorySummary(1L, "Salary", BigDecimal("42000.00"))),
        expensesByCategory = listOf(CategorySummary(3L, "Food", BigDecimal("12000.00")))
    )

    @Test
    fun `GET monthly report - returns 401 for unauthenticated`() {
        mockMvc.perform(get("/api/reports/monthly/2024/3"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "test@example.com")
    fun `GET monthly report - returns report for valid year and month`() {
        whenever(authService.findUserByUsername("test@example.com")).thenReturn(testUser)
        whenever(reportService.getMonthlyReport(testUser, 2024, 3)).thenReturn(sampleMonthlyReport)

        mockMvc.perform(get("/api/reports/monthly/2024/3"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.year").value(2024))
            .andExpect(jsonPath("$.month").value(3))
            .andExpect(jsonPath("$.totalIncome").value(3500.00))
            .andExpect(jsonPath("$.totalExpenses").value(1000.00))
            .andExpect(jsonPath("$.netSavings").value(2500.00))
            .andExpect(jsonPath("$.incomeByCategory[0].categoryName").value("Salary"))
            .andExpect(jsonPath("$.expensesByCategory[0].categoryName").value("Food"))
    }

    @Test
    @WithMockUser(username = "test@example.com")
    fun `GET monthly report - returns 400 for invalid month`() {
        whenever(authService.findUserByUsername("test@example.com")).thenReturn(testUser)
        whenever(reportService.getMonthlyReport(testUser, 2024, 13))
            .thenThrow(BadRequestException("Month must be between 1 and 12"))

        mockMvc.perform(get("/api/reports/monthly/2024/13"))
            .andExpect(status().isBadRequest)
    }

    @Test
    @WithMockUser(username = "test@example.com")
    fun `GET monthly report - returns 400 for invalid year`() {
        whenever(authService.findUserByUsername("test@example.com")).thenReturn(testUser)
        whenever(reportService.getMonthlyReport(testUser, 1999, 1))
            .thenThrow(BadRequestException("Year must be between 2000 and 2100"))

        mockMvc.perform(get("/api/reports/monthly/1999/1"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `GET yearly report - returns 401 for unauthenticated`() {
        mockMvc.perform(get("/api/reports/yearly/2024"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "test@example.com")
    fun `GET yearly report - returns report for valid year`() {
        whenever(authService.findUserByUsername("test@example.com")).thenReturn(testUser)
        whenever(reportService.getYearlyReport(testUser, 2024)).thenReturn(sampleYearlyReport)

        mockMvc.perform(get("/api/reports/yearly/2024"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.year").value(2024))
            .andExpect(jsonPath("$.totalIncome").value(42000.00))
            .andExpect(jsonPath("$.totalExpenses").value(12000.00))
            .andExpect(jsonPath("$.netSavings").value(30000.00))
            .andExpect(jsonPath("$.monthlyBreakdown.length()").value(12))
    }

    @Test
    @WithMockUser(username = "test@example.com")
    fun `GET yearly report - returns 400 for invalid year`() {
        whenever(authService.findUserByUsername("test@example.com")).thenReturn(testUser)
        whenever(reportService.getYearlyReport(testUser, 1999))
            .thenThrow(BadRequestException("Invalid year"))

        mockMvc.perform(get("/api/reports/yearly/1999"))
            .andExpect(status().isBadRequest)
    }
}
