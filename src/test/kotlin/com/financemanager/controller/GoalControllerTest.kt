package com.financemanager.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.financemanager.config.SecurityConfig
import com.financemanager.dto.CreateGoalRequest
import com.financemanager.dto.GoalResponse
import com.financemanager.dto.UpdateGoalRequest
import com.financemanager.entity.User
import com.financemanager.exception.BadRequestException
import com.financemanager.exception.GlobalExceptionHandler
import com.financemanager.exception.ResourceNotFoundException
import com.financemanager.security.UserDetailsServiceImpl
import com.financemanager.service.AuthService
import com.financemanager.service.GoalService
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

@WebMvcTest(GoalController::class)
@Import(SecurityConfig::class, GlobalExceptionHandler::class)
class GoalControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var goalService: GoalService

    @MockBean
    private lateinit var authService: AuthService

    @MockBean
    private lateinit var userDetailsService: UserDetailsServiceImpl

    private val testUser = User(id = 1L, username = "test@example.com", password = "hashed", fullName = "Test", phoneNumber = "123")
    private val futureDate = LocalDate.now().plusMonths(6)

    private val sampleGoal = GoalResponse(
        id = 1L,
        goalName = "Emergency Fund",
        targetAmount = BigDecimal("5000.00"),
        targetDate = futureDate,
        startDate = LocalDate.now(),
        currentProgress = BigDecimal("1000.00"),
        progressPercentage = BigDecimal("20.00"),
        remainingAmount = BigDecimal("4000.00")
    )

    @Test
    fun `GET goals - returns 401 for unauthenticated`() {
        mockMvc.perform(get("/api/goals"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "test@example.com")
    fun `GET goals - returns list for authenticated user`() {
        whenever(authService.findUserByUsername("test@example.com")).thenReturn(testUser)
        whenever(goalService.getGoals(testUser)).thenReturn(listOf(sampleGoal))

        mockMvc.perform(get("/api/goals"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].goalName").value("Emergency Fund"))
            .andExpect(jsonPath("$[0].currentProgress").value(1000.00))
            .andExpect(jsonPath("$[0].progressPercentage").value(20.00))
    }

    @Test
    @WithMockUser(username = "test@example.com")
    fun `get goal by id returns single goal`() {
        whenever(authService.findUserByUsername("test@example.com")).thenReturn(testUser)
        whenever(goalService.getGoal(testUser, 1L)).thenReturn(sampleGoal)

        mockMvc.perform(get("/api/goals/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.goalName").value("Emergency Fund"))
            .andExpect(jsonPath("$.remainingAmount").value(4000.00))
    }

    @Test
    @WithMockUser(username = "test@example.com")
    fun `get goal by id returns 404 when not found`() {
        whenever(authService.findUserByUsername("test@example.com")).thenReturn(testUser)
        whenever(goalService.getGoal(testUser, 99L)).thenThrow(ResourceNotFoundException("Goal not found"))

        mockMvc.perform(get("/api/goals/99"))
            .andExpect(status().isNotFound)
    }

    @Test
    @WithMockUser(username = "test@example.com")
    fun `POST goals - returns 201 on success`() {
        val request = CreateGoalRequest(
            goalName = "Emergency Fund",
            targetAmount = BigDecimal("5000.00"),
            targetDate = futureDate
        )
        whenever(authService.findUserByUsername("test@example.com")).thenReturn(testUser)
        whenever(goalService.createGoal(eq(testUser), any())).thenReturn(sampleGoal)

        mockMvc.perform(
            post("/api/goals")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.goalName").value("Emergency Fund"))
            .andExpect(jsonPath("$.targetAmount").value(5000.00))
    }

    @Test
    @WithMockUser(username = "test@example.com")
    fun `POST goals - returns 400 for past target date`() {
        val request = CreateGoalRequest(
            goalName = "Test",
            targetAmount = BigDecimal("1000.00"),
            targetDate = LocalDate.now().minusDays(1)
        )
        whenever(authService.findUserByUsername("test@example.com")).thenReturn(testUser)
        whenever(goalService.createGoal(eq(testUser), any()))
            .thenThrow(BadRequestException("Target date must be future"))

        mockMvc.perform(
            post("/api/goals")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @WithMockUser(username = "test@example.com")
    fun `POST goals - returns 400 for negative amount`() {
        val request = mapOf(
            "goalName" to "Test Goal",
            "targetAmount" to -100,
            "targetDate" to futureDate.toString()
        )

        mockMvc.perform(
            post("/api/goals")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @WithMockUser(username = "test@example.com")
    fun `PUT goals - returns 200 on success`() {
        val request = UpdateGoalRequest(
            goalName = "Updated Fund",
            targetAmount = BigDecimal("10000.00"),
            targetDate = futureDate.plusMonths(6)
        )
        val updated = sampleGoal.copy(goalName = "Updated Fund", targetAmount = BigDecimal("10000.00"))

        whenever(authService.findUserByUsername("test@example.com")).thenReturn(testUser)
        whenever(goalService.updateGoal(eq(testUser), eq(1L), any())).thenReturn(updated)

        mockMvc.perform(
            put("/api/goals/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.goalName").value("Updated Fund"))
    }

    @Test
    @WithMockUser(username = "test@example.com")
    fun `DELETE goals - returns 200 on success`() {
        whenever(authService.findUserByUsername("test@example.com")).thenReturn(testUser)
        doNothing().whenever(goalService).deleteGoal(testUser, 1L)

        mockMvc.perform(delete("/api/goals/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").exists())
    }

    @Test
    @WithMockUser(username = "test@example.com")
    fun `DELETE goals - returns 404 when not found`() {
        whenever(authService.findUserByUsername("test@example.com")).thenReturn(testUser)
        whenever(goalService.deleteGoal(testUser, 99L)).thenThrow(ResourceNotFoundException("Goal not found"))

        mockMvc.perform(delete("/api/goals/99"))
            .andExpect(status().isNotFound)
    }
}
