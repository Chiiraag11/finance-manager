package com.financemanager.service

import com.financemanager.dto.LoginRequest
import com.financemanager.dto.RegisterRequest
import com.financemanager.dto.UserResponse
import com.financemanager.entity.User
import com.financemanager.exception.DuplicateResourceException
import com.financemanager.exception.ResourceNotFoundException
import com.financemanager.repository.UserRepository
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.context.SecurityContextRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val authenticationManager: AuthenticationManager,
    private val securityContextRepository: SecurityContextRepository
) {

    @Transactional
    fun register(request: RegisterRequest): UserResponse {
        if (userRepository.existsByUsername(request.username.lowercase())) {
            throw DuplicateResourceException("An account with email '${request.username}' already exists")
        }

        val user = User(
            username = request.username.lowercase(),
            password = passwordEncoder.encode(request.password),
            fullName = request.fullName,
            phoneNumber = request.phoneNumber
        )

        val saved = userRepository.save(user)
        return saved.toResponse()
    }

    fun login(
        request: LoginRequest,
        httpRequest: HttpServletRequest,
        httpResponse: HttpServletResponse
    ): UserResponse {
        val authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(request.username.lowercase(), request.password)
        )

        val context = SecurityContextHolder.createEmptyContext()
        context.authentication = authentication
        SecurityContextHolder.setContext(context)
        securityContextRepository.saveContext(context, httpRequest, httpResponse)

        val user = userRepository.findByUsername(request.username.lowercase())
            .orElseThrow { ResourceNotFoundException("User not found") }

        return user.toResponse()
    }

    fun logout(httpRequest: HttpServletRequest) {
        httpRequest.getSession(false)?.invalidate()
        SecurityContextHolder.clearContext()
    }

    @Transactional(readOnly = true)
    fun findUserByUsername(username: String): User =
        userRepository.findByUsername(username)
            .orElseThrow { ResourceNotFoundException("User not found: $username") }

    private fun User.toResponse() = UserResponse(
        id = id,
        username = username,
        fullName = fullName,
        phoneNumber = phoneNumber
    )
}
