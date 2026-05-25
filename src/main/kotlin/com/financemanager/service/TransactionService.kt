package com.financemanager.service

import com.financemanager.dto.*
import com.financemanager.entity.Transaction
import com.financemanager.entity.User
import com.financemanager.exception.BadRequestException
import com.financemanager.exception.ResourceNotFoundException
import com.financemanager.repository.TransactionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class TransactionService(
    private val transactionRepository: TransactionRepository,
    private val categoryService: CategoryService
) {

    @Transactional
    fun createTransaction(user: User, request: CreateTransactionRequest): TransactionResponse {
        validateDate(request.date)

        val category = categoryService.findCategoryByNameForUser(
            request.category,
            user
        )

        val transaction = Transaction(
            amount = request.amount,
            date = request.date,
            category = category,
            description = request.description?.trim(),
            user = user
        )

        return transactionRepository.save(transaction).toResponse()
    }

    @Transactional(readOnly = true)
    fun getTransactions(user: User, filters: TransactionFilterRequest): List<TransactionResponse> {

        if (
            filters.startDate != null &&
            filters.endDate != null &&
            filters.startDate.isAfter(filters.endDate)
        ) {
            throw BadRequestException("startDate must not be after endDate")
        }

        return transactionRepository.findByUserWithFilters(
            user = user,
            startDate = filters.startDate,
            endDate = filters.endDate,
            categoryId = filters.categoryId
        ).map { it.toResponse() }
    }

    @Transactional
    fun updateTransaction(
        user: User,
        id: Long,
        request: UpdateTransactionRequest
    ): TransactionResponse {

        val transaction = transactionRepository
            .findByIdAndUserAndDeletedFalse(id, user)
            .orElseThrow {
                ResourceNotFoundException("Transaction with id $id not found")
            }

        val category = categoryService.findCategoryByNameForUser(
            request.category,
            user
        )

        transaction.amount = request.amount
        transaction.category = category
        transaction.description = request.description?.trim()

        return transactionRepository.save(transaction).toResponse()
    }

    @Transactional
    fun deleteTransaction(user: User, id: Long) {

        val transaction = transactionRepository
            .findByIdAndUserAndDeletedFalse(id, user)
            .orElseThrow {
                ResourceNotFoundException("Transaction with id $id not found")
            }

        transaction.deleted = true

        transactionRepository.save(transaction)
    }

    private fun validateDate(date: LocalDate) {

        if (date.isAfter(LocalDate.now())) {
            throw BadRequestException(
                "Transaction date cannot be a future date"
            )
        }
    }

    private fun Transaction.toResponse() = TransactionResponse(
        id = id,
        amount = amount,
        date = date,
        categoryId = category.id,
        categoryName = category.name,
        categoryType = category.type,
        description = description
    )
}