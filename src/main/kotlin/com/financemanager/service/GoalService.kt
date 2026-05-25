package com.financemanager.service

import com.financemanager.dto.*
import com.financemanager.entity.CategoryType
import com.financemanager.entity.Goal
import com.financemanager.entity.User
import com.financemanager.exception.BadRequestException
import com.financemanager.exception.ResourceNotFoundException
import com.financemanager.repository.GoalRepository
import com.financemanager.repository.TransactionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

@Service
class GoalService(
    private val goalRepository: GoalRepository,
    private val transactionRepository: TransactionRepository
) {

    @Transactional
    fun createGoal(
        user: User,
        request: CreateGoalRequest
    ): GoalResponse {

        validateTargetDate(request.targetDate)

        val startDate = request.startDate ?: LocalDate.now()

        if (
            request.startDate != null &&
            request.startDate.isAfter(request.targetDate)
        ) {
            throw BadRequestException(
                "Start date cannot be after target date"
            )
        }

        val goal = Goal(
            goalName = request.goalName.trim(),
            targetAmount = request.targetAmount,
            targetDate = request.targetDate,
            startDate = startDate,
            user = user
        )

        val saved = goalRepository.save(goal)

        return buildGoalResponse(saved, user)
    }

    @Transactional(readOnly = true)
    fun getGoals(user: User): List<GoalResponse> =
        goalRepository
            .findByUserOrderByTargetDateAsc(user)
            .map { buildGoalResponse(it, user) }

    @Transactional(readOnly = true)
    fun getGoal(
        user: User,
        id: Long
    ): GoalResponse {

        val goal = goalRepository
            .findByIdAndUser(id, user)
            .orElseThrow {
                ResourceNotFoundException(
                    "Goal with id $id not found"
                )
            }

        return buildGoalResponse(goal, user)
    }

    @Transactional
    fun updateGoal(
        user: User,
        id: Long,
        request: UpdateGoalRequest
    ): GoalResponse {

        val goal = goalRepository
            .findByIdAndUser(id, user)
            .orElseThrow {
                ResourceNotFoundException(
                    "Goal with id $id not found"
                )
            }

        request.goalName?.let {
            goal.goalName = it.trim()
        }

        request.targetAmount?.let {
            goal.targetAmount = it
        }

        request.targetDate?.let {
            validateTargetDate(it)
            goal.targetDate = it
        }

        val saved = goalRepository.save(goal)

        return buildGoalResponse(saved, user)
    }

    @Transactional
    fun deleteGoal(
        user: User,
        id: Long
    ) {

        val goal = goalRepository
            .findByIdAndUser(id, user)
            .orElseThrow {
                ResourceNotFoundException(
                    "Goal with id $id not found"
                )
            }

        goalRepository.delete(goal)
    }

    private fun buildGoalResponse(
        goal: Goal,
        user: User
    ): GoalResponse {

        val transactions = transactionRepository
            .findByUserSinceDate(user, goal.startDate)

        val totalIncome = transactions
            .filter {
                it.category.type == CategoryType.INCOME
            }
            .fold(BigDecimal.ZERO) { acc, t ->
                acc + t.amount
            }

        val totalExpenses = transactions
            .filter {
                it.category.type == CategoryType.EXPENSE
            }
            .fold(BigDecimal.ZERO) { acc, t ->
                acc + t.amount
            }

        val currentProgress =
            (totalIncome - totalExpenses)
                .coerceAtLeast(BigDecimal.ZERO)

        val progressPercentage =
            if (goal.targetAmount > BigDecimal.ZERO) {

                (
                    currentProgress.divide(
                        goal.targetAmount,
                        4,
                        RoundingMode.HALF_UP
                    ) * BigDecimal("100")
                )
                    .setScale(2, RoundingMode.HALF_UP)
                    .coerceAtMost(BigDecimal("100.00"))

            } else {
                BigDecimal.ZERO
            }

        val remainingAmount =
            (goal.targetAmount - currentProgress)
                .coerceAtLeast(BigDecimal.ZERO)

        return GoalResponse(
            id = goal.id,
            goalName = goal.goalName,
            targetAmount = goal.targetAmount,
            targetDate = goal.targetDate,
            startDate = goal.startDate,
            currentProgress = currentProgress.setScale(
                2,
                RoundingMode.HALF_UP
            ),
            progressPercentage = progressPercentage,
            remainingAmount = remainingAmount.setScale(
                2,
                RoundingMode.HALF_UP
            )
        )
    }

    private fun validateTargetDate(targetDate: LocalDate) {

        if (!targetDate.isAfter(LocalDate.now())) {
            throw BadRequestException(
                "Target date must be a future date"
            )
        }
    }
}