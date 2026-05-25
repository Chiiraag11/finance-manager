package com.financemanager.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.financemanager.config.SecurityConfig
import com.financemanager.dto.LoginRequest
import com.financemanager.dto.RegisterRequest
import com.financemanager.dto.UserResponse
import com.financemanager.exception.DuplicateResourceException
import com.financemanager.exception.GlobalExceptionHandler
import com.financemanager.security.UserDetailsServiceImpl
import com.financemanager.service.AuthService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(AuthController::class)
@Import(SecurityConfig::class, GlobalExceptionHandler::class)
class AuthControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var authService: AuthService

    @MockBean
    private lateinit var userDetailsService: UserDetailsServiceImpl

    private val sampleUser = UserResponse(id = 1L, username = "test@example.com", fullName = "Test User", phoneNumber = "+1234567890")

    @Test
    fun `POST register - returns 201 on success`() {
        val request = RegisterRequest(
            username = "test@example.com",
            password = "password123",
            fullName = "Test User",
            phoneNumber = "+1234567890"
        )
        whenever(authService.register(any())).thenReturn(sampleUser)

        mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.username").value("test@example.com"))
            .andExpect(jsonPath("$.fullName").value("Test User"))
            .andExpect(jsonPath("$.id").value(1))
    }

    @Test
    fun `POST register - returns 409 when email already exists`() {
        val request = RegisterRequest(
            username = "existing@example.com",
            password = "password123",
            fullName = "Test User",
            phoneNumber = "+1234567890"
        )
        whenever(authService.register(any())).thenThrow(DuplicateResourceException("Email already exists"))

        mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error").value("Conflict"))
    }

    @Test
    fun `POST register - returns 400 for invalid email`() {
        val request = mapOf(
            "username" to "not-an-email",
            "password" to "password123",
            "fullName" to "Test User",
            "phoneNumber" to "+1234567890"
        )

        mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `POST register - returns 400 for short password`() {
        val request = mapOf(
            "username" to "test@example.com",
            "password" to "short",
            "fullName" to "Test User",
            "phoneNumber" to "+1234567890"
        )

        mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `POST register - returns 400 for missing required fields`() {
        val request = mapOf("username" to "test@example.com")

        mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `POST login - returns 200 on success`() {
        val request = LoginRequest(username = "test@example.com", password = "password123")
        whenever(authService.login(any(), any(), any())).thenReturn(sampleUser)

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.username").value("test@example.com"))
    }

    @Test
    fun `POST login - returns 401 for bad credentials`() {
        val request = LoginRequest(username = "test@example.com", password = "wrongpassword")
        whenever(authService.login(any(), any<HttpServletRequest>(), any<HttpServletResponse>()))
            .thenThrow(BadCredentialsException("Invalid credentials"))

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `POST login - returns 400 for missing fields`() {
        val request = mapOf("username" to "test@example.com")

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @WithMockUser
    fun `POST logout - returns 200 for authenticated user`() {
        doNothing().whenever(authService).logout(any())

        mockMvc.perform(post("/api/auth/logout"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("Logged out successfully"))
    }

    @Test
    fun `POST logout - returns 401 for unauthenticated user`() {
        mockMvc.perform(post("/api/auth/logout"))
            .andExpect(status().isUnauthorized)
    }
}
