package com.financemanager.service

import com.financemanager.dto.LoginRequest
import com.financemanager.dto.RegisterRequest
import com.financemanager.entity.User
import com.financemanager.exception.DuplicateResourceException
import com.financemanager.exception.ResourceNotFoundException
import com.financemanager.repository.UserRepository
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpSession
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.context.SecurityContextRepository
import java.util.Optional

class AuthServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var authenticationManager: AuthenticationManager
    private lateinit var securityContextRepository: SecurityContextRepository
    private lateinit var authService: AuthService

    @BeforeEach
    fun setUp() {
        userRepository = mock()
        passwordEncoder = mock()
        authenticationManager = mock()
        securityContextRepository = mock()
        authService = AuthService(userRepository, passwordEncoder, authenticationManager, securityContextRepository)
    }

    @Test
    fun `register - success creates new user`() {
        val request = RegisterRequest(
            username = "test@example.com",
            password = "password123",
            fullName = "Test User",
            phoneNumber = "+1234567890"
        )
        val savedUser = User(id = 1L, username = "test@example.com", password = "hashed", fullName = "Test User", phoneNumber = "+1234567890")

        whenever(userRepository.existsByUsername("test@example.com")).thenReturn(false)
        whenever(passwordEncoder.encode("password123")).thenReturn("hashed")
        whenever(userRepository.save(any())).thenReturn(savedUser)

        val result = authService.register(request)

        assertEquals("test@example.com", result.username)
        assertEquals("Test User", result.fullName)
        verify(userRepository).save(any())
    }

    @Test
    fun `register - converts email to lowercase`() {
        val request = RegisterRequest(
            username = "TEST@EXAMPLE.COM",
            password = "password123",
            fullName = "Test User",
            phoneNumber = "+1234567890"
        )
        val savedUser = User(id = 1L, username = "test@example.com", password = "hashed", fullName = "Test User", phoneNumber = "+1234567890")

        whenever(userRepository.existsByUsername("test@example.com")).thenReturn(false)
        whenever(passwordEncoder.encode(any())).thenReturn("hashed")
        whenever(userRepository.save(any())).thenReturn(savedUser)

        authService.register(request)

        verify(userRepository).existsByUsername("test@example.com")
    }

    @Test
    fun `register - throws DuplicateResourceException when email exists`() {
        val request = RegisterRequest(
            username = "existing@example.com",
            password = "password123",
            fullName = "Test User",
            phoneNumber = "+1234567890"
        )
        whenever(userRepository.existsByUsername("existing@example.com")).thenReturn(true)

        assertThrows<DuplicateResourceException> { authService.register(request) }
        verify(userRepository, never()).save(any())
    }

    @Test
    fun `login - success returns user response`() {
        val request = LoginRequest(username = "test@example.com", password = "password123")
        val user = User(id = 1L, username = "test@example.com", password = "hashed", fullName = "Test User", phoneNumber = "+1234567890")
        val authentication = mock<Authentication>()
        val httpRequest = mock<HttpServletRequest>()
        val httpResponse = mock<HttpServletResponse>()
        val session = mock<HttpSession>()

        whenever(authenticationManager.authenticate(any())).thenReturn(authentication)
        whenever(userRepository.findByUsername("test@example.com")).thenReturn(Optional.of(user))
        whenever(httpRequest.getSession(true)).thenReturn(session)

        val result = authService.login(request, httpRequest, httpResponse)

        assertEquals("test@example.com", result.username)
        verify(securityContextRepository).saveContext(any(), eq(httpRequest), eq(httpResponse))
    }

    @Test
    fun `login - throws BadCredentialsException for wrong password`() {
        val request = LoginRequest(username = "test@example.com", password = "wrongpass")
        val httpRequest = mock<HttpServletRequest>()
        val httpResponse = mock<HttpServletResponse>()

        whenever(authenticationManager.authenticate(any())).thenThrow(BadCredentialsException("Bad credentials"))

        assertThrows<BadCredentialsException> {
            authService.login(request, httpRequest, httpResponse)
        }
    }

    @Test
    fun `logout - invalidates session`() {
        val httpRequest = mock<HttpServletRequest>()
        val session = mock<HttpSession>()

        whenever(httpRequest.getSession(false)).thenReturn(session)

        authService.logout(httpRequest)

        verify(session).invalidate()
    }

    @Test
    fun `logout - handles null session gracefully`() {
        val httpRequest = mock<HttpServletRequest>()
        whenever(httpRequest.getSession(false)).thenReturn(null)

        assertDoesNotThrow { authService.logout(httpRequest) }
    }

    @Test
    fun `findUserByUsername - returns user when found`() {
        val user = User(id = 1L, username = "test@example.com", password = "hashed", fullName = "Test User", phoneNumber = "+1234567890")
        whenever(userRepository.findByUsername("test@example.com")).thenReturn(Optional.of(user))

        val result = authService.findUserByUsername("test@example.com")
        assertEquals(user, result)
    }

    @Test
    fun `findUserByUsername - throws ResourceNotFoundException when not found`() {
        whenever(userRepository.findByUsername("unknown@example.com")).thenReturn(Optional.empty())
        assertThrows<ResourceNotFoundException> { authService.findUserByUsername("unknown@example.com") }
    }
}
