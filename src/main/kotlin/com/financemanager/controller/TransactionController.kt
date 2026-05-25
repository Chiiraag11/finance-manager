package com.financemanager.controller

import com.financemanager.dto.CreateTransactionRequest
import com.financemanager.dto.MessageResponse
import com.financemanager.dto.TransactionFilterRequest
import com.financemanager.dto.TransactionResponse
import com.financemanager.dto.UpdateTransactionRequest
import com.financemanager.service.AuthService
import com.financemanager.service.TransactionService
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/transactions")
class TransactionController(
    private val transactionService: TransactionService,
    private val authService: AuthService
) {

    @PostMapping
    fun createTransaction(
        @AuthenticationPrincipal userDetails: UserDetails,
        @Valid @RequestBody request: CreateTransactionRequest
    ): ResponseEntity<TransactionResponse> {
        val user = authService.findUserByUsername(userDetails.username)
        val transaction = transactionService.createTransaction(user, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(transaction)
    }

    @GetMapping
    fun getTransactions(
        @AuthenticationPrincipal userDetails: UserDetails,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate?,
        @RequestParam(required = false) categoryId: Long?
    ): ResponseEntity<List<TransactionResponse>> {
        val user = authService.findUserByUsername(userDetails.username)
        val filters = TransactionFilterRequest(startDate, endDate, categoryId)
        return ResponseEntity.ok(transactionService.getTransactions(user, filters))
    }

    @PutMapping("/{id}")
    fun updateTransaction(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateTransactionRequest
    ): ResponseEntity<TransactionResponse> {
        val user = authService.findUserByUsername(userDetails.username)
        val transaction = transactionService.updateTransaction(user, id, request)
        return ResponseEntity.ok(transaction)
    }

    @DeleteMapping("/{id}")
    fun deleteTransaction(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable id: Long
    ): ResponseEntity<MessageResponse> {
        val user = authService.findUserByUsername(userDetails.username)
        transactionService.deleteTransaction(user, id)
        return ResponseEntity.ok(MessageResponse("Transaction deleted successfully"))
    }
}
