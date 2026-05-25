package com.financemanager.controller

import com.financemanager.dto.LoginRequest
import com.financemanager.dto.MessageResponse
import com.financemanager.dto.RegisterRequest
import com.financemanager.dto.UserResponse
import com.financemanager.service.AuthService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(private val authService: AuthService) {

    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<UserResponse> {
        val user = authService.register(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(user)
    }

    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest,
        httpRequest: HttpServletRequest,
        httpResponse: HttpServletResponse
    ): ResponseEntity<UserResponse> {
        val user = authService.login(request, httpRequest, httpResponse)
        return ResponseEntity.ok(user)
    }

    @PostMapping("/logout")
    fun logout(httpRequest: HttpServletRequest): ResponseEntity<MessageResponse> {
        authService.logout(httpRequest)
        return ResponseEntity.ok(MessageResponse("Logged out successfully"))
    }
}
