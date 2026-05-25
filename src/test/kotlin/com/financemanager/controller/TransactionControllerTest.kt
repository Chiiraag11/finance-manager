package com.financemanager.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.financemanager.config.SecurityConfig
import com.financemanager.dto.CreateTransactionRequest
import com.financemanager.dto.TransactionResponse
import com.financemanager.dto.UpdateTransactionRequest
import com.financemanager.entity.CategoryType
import com.financemanager.entity.User
import com.financemanager.exception.BadRequestException
import com.financemanager.exception.GlobalExceptionHandler
import com.financemanager.exception.ResourceNotFoundException
import com.financemanager.security.UserDetailsServiceImpl
import com.financemanager.service.AuthService
import com.financemanager.service.TransactionService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.math.BigDecimal
import java.time.LocalDate

@WebMvcTest(TransactionController::class)
@Import(SecurityConfig::class, GlobalExceptionHandler::class)
class TransactionControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var transactionService: TransactionService

    @MockBean
    private lateinit var authService: AuthService

    @MockBean
    private lateinit var userDetailsService: UserDetailsServiceImpl

    private val testUser = User(id = 1L, username = "test@example.com", password = "hashed", fullName = "Test", phoneNumber = "123")

    private val sampleTransaction = TransactionResponse(
        id = 1L,
        amount = BigDecimal("1500.00"),
        date = LocalDate.of(2024, 3, 15),
        categoryId = 1L,
        categoryName = "Salary",
        categoryType = CategoryType.INCOME,
        description = "Monthly salary"
    )

    @Test
    fun `GET transactions - returns 401 for unauthenticated`() {
        mockMvc.perform(get("/api/transactions"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "test@example.com")
    fun `GET transactions - returns list for authenticated user`() {
        whenever(authService.findUserByUsername("test@example.com")).thenReturn(testUser)
        whenever(transactionService.getTransactions(eq(testUser), any())).thenReturn(listOf(sampleTransaction))

        mockMvc.perform(get("/api/transactions"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].amount").value(1500.00))
            .andExpect(jsonPath("$[0].categoryName").value("Salary"))
    }

    @Test
    @WithMockUser(username = "test@example.com")
    fun `GET transactions - passes filters to service`() {
        whenever(authService.findUserByUsername("test@example.com")).thenReturn(testUser)
        whenever(transactionService.getTransactions(eq(testUser), any())).thenReturn(emptyList())

        mockMvc.perform(
            get("/api/transactions")
                .param("startDate", "2024-01-01")
                .param("endDate", "2024-01-31")
                .param("categoryId", "1")
        )
            .andExpect(status().isOk)

        verify(transactionService).getTransactions(
            eq(testUser),
            argThat { startDate == LocalDate.of(2024, 1, 1) && endDate == LocalDate.of(2024, 1, 31) && categoryId == 1L }
        )
    }

    @Test
    @WithMockUser(username = "test@example.com")
    fun `POST transactions - returns 201 on success`() {
        val request = CreateTransactionRequest(
            amount = BigDecimal("1500.00"),
            date = LocalDate.now().minusDays(1),
            categoryId = 1L,
            description = "Monthly salary"
        )
        whenever(authService.findUserByUsername("test@example.com")).thenReturn(testUser)
        whenever(transactionService.createTransaction(eq(testUser), any())).thenReturn(sampleTransaction)

        mockMvc.perform(
            post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.amount").value(1500.00))
    }

    @Test
    @WithMockUser(username = "test@example.com")
    fun `POST transactions - returns 400 for future date`() {
        val request = CreateTransactionRequest(
            amount = BigDecimal("100.00"),
            date = LocalDate.now().plusDays(1),
            categoryId = 1L
        )
        whenever(authService.findUserByUsername("test@example.com")).thenReturn(testUser)
        whenever(transactionService.createTransaction(eq(testUser), any()))
            .thenThrow(BadRequestException("Transaction date cannot be a future date"))

        mockMvc.perform(
            post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @WithMockUser(username = "test@example.com")
    fun `POST transactions - returns 400 for negative amount`() {
        val request = mapOf(
            "amount" to -100,
            "date" to LocalDate.now().toString(),
            "categoryId" to 1
        )

        mockMvc.perform(
            post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @WithMockUser(username = "test@example.com")
    fun `PUT transactions - returns 200 on success`() {
        val request = UpdateTransactionRequest(
            amount = BigDecimal("2000.00"),
            categoryId = 1L,
            description = "Updated"
        )
        val updated = sampleTransaction.copy(amount = BigDecimal("2000.00"))

        whenever(authService.findUserByUsername("test@example.com")).thenReturn(testUser)
        whenever(transactionService.updateTransaction(eq(testUser), eq(1L), any())).thenReturn(updated)

        mockMvc.perform(
            put("/api/transactions/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.amount").value(2000.00))
    }

    @Test
    @WithMockUser(username = "test@example.com")
    fun `PUT transactions - returns 404 when not found`() {
        val request = UpdateTransactionRequest(amount = BigDecimal("100.00"), categoryId = 1L)

        whenever(authService.findUserByUsername("test@example.com")).thenReturn(testUser)
        whenever(transactionService.updateTransaction(eq(testUser), eq(99L), any()))
            .thenThrow(ResourceNotFoundException("Transaction not found"))

        mockMvc.perform(
            put("/api/transactions/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isNotFound)
    }

    @Test
    @WithMockUser(username = "test@example.com")
    fun `DELETE transactions - returns 200 on success`() {
        whenever(authService.findUserByUsername("test@example.com")).thenReturn(testUser)
        doNothing().whenever(transactionService).deleteTransaction(testUser, 1L)

        mockMvc.perform(delete("/api/transactions/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").exists())
    }

    @Test
    @WithMockUser(username = "test@example.com")
    fun `DELETE transactions - returns 404 when not found`() {
        whenever(authService.findUserByUsername("test@example.com")).thenReturn(testUser)
        whenever(transactionService.deleteTransaction(testUser, 99L))
            .thenThrow(ResourceNotFoundException("Transaction not found"))

        mockMvc.perform(delete("/api/transactions/99"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `DELETE transactions - returns 401 for unauthenticated`() {
        mockMvc.perform(delete("/api/transactions/1"))
            .andExpect(status().isUnauthorized)
    }
}
