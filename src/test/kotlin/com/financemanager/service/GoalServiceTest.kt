package com.financemanager.service

import com.financemanager.dto.CreateGoalRequest
import com.financemanager.dto.UpdateGoalRequest
import com.financemanager.entity.Category
import com.financemanager.entity.CategoryType
import com.financemanager.entity.Goal
import com.financemanager.entity.Transaction
import com.financemanager.entity.User
import com.financemanager.exception.BadRequestException
import com.financemanager.exception.ResourceNotFoundException
import com.financemanager.repository.GoalRepository
import com.financemanager.repository.TransactionRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional

class GoalServiceTest {

    private lateinit var goalRepository: GoalRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var goalService: GoalService

    private val testUser = User(id = 1L, username = "test@example.com", password = "hashed", fullName = "Test", phoneNumber = "123")
    private val salaryCategory = Category(id = 1L, name = "Salary", type = CategoryType.INCOME, isDefault = true)
    private val foodCategory = Category(id = 2L, name = "Food", type = CategoryType.EXPENSE, isDefault = true)
    private val futureDate = LocalDate.now().plusMonths(6)
    private val startDate = LocalDate.now().minusMonths(1)

    @BeforeEach
    fun setUp() {
        goalRepository = mock()
        transactionRepository = mock()
        goalService = GoalService(goalRepository, transactionRepository)
    }

    @Test
    fun `createGoal - success creates goal with default startDate`() {
        val request = CreateGoalRequest(
            goalName = "Emergency Fund",
            targetAmount = BigDecimal("5000.00"),
            targetDate = futureDate
        )
        val saved = Goal(id = 1L, goalName = "Emergency Fund", targetAmount = BigDecimal("5000.00"), targetDate = futureDate, startDate = LocalDate.now(), user = testUser)

        whenever(goalRepository.save(any())).thenReturn(saved)
        whenever(transactionRepository.findByUserSinceDate(eq(testUser), any())).thenReturn(emptyList())

        val result = goalService.createGoal(testUser, request)

        assertEquals("Emergency Fund", result.goalName)
        assertEquals(BigDecimal("5000.00"), result.targetAmount)
        assertEquals(BigDecimal("0.00"), result.currentProgress)
    }

    @Test
    fun `createGoal - throws BadRequestException when targetDate is not future`() {
        val request = CreateGoalRequest(
            goalName = "Test Goal",
            targetAmount = BigDecimal("1000.00"),
            targetDate = LocalDate.now()
        )

        assertThrows<BadRequestException> { goalService.createGoal(testUser, request) }
    }

    @Test
    fun `createGoal - throws BadRequestException when startDate after targetDate`() {
        val request = CreateGoalRequest(
            goalName = "Test Goal",
            targetAmount = BigDecimal("1000.00"),
            targetDate = LocalDate.now().plusDays(5),
            startDate = LocalDate.now().plusDays(10)
        )

        assertThrows<BadRequestException> { goalService.createGoal(testUser, request) }
    }

    @Test
    fun `getGoal - calculates progress correctly`() {
        val goal = Goal(id = 1L, goalName = "Vacation Fund", targetAmount = BigDecimal("2000.00"), targetDate = futureDate, startDate = startDate, user = testUser)
        val incomeTx = Transaction(id = 1L, amount = BigDecimal("3000.00"), date = LocalDate.now(), category = salaryCategory, user = testUser)
        val expenseTx = Transaction(id = 2L, amount = BigDecimal("500.00"), date = LocalDate.now(), category = foodCategory, user = testUser)

        whenever(goalRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.of(goal))
        whenever(transactionRepository.findByUserSinceDate(testUser, startDate)).thenReturn(listOf(incomeTx, expenseTx))

        val result = goalService.getGoal(testUser, 1L)

        assertEquals(BigDecimal("2500.00"), result.currentProgress)
        assertEquals(BigDecimal("100.00"), result.progressPercentage) // 2500/2000 > 100, capped at 100
        assertEquals(BigDecimal("0.00"), result.remainingAmount)
    }

    @Test
    fun `getGoal - progress is zero when expenses exceed income`() {
        val goal = Goal(id = 1L, goalName = "Savings", targetAmount = BigDecimal("1000.00"), targetDate = futureDate, startDate = startDate, user = testUser)
        val expenseTx = Transaction(id = 1L, amount = BigDecimal("1000.00"), date = LocalDate.now(), category = foodCategory, user = testUser)

        whenever(goalRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.of(goal))
        whenever(transactionRepository.findByUserSinceDate(testUser, startDate)).thenReturn(listOf(expenseTx))

        val result = goalService.getGoal(testUser, 1L)

        assertEquals(BigDecimal("0.00"), result.currentProgress)
        assertEquals(BigDecimal("1000.00"), result.remainingAmount)
    }

    @Test
    fun `getGoal - throws ResourceNotFoundException when not found`() {
        whenever(goalRepository.findByIdAndUser(99L, testUser)).thenReturn(Optional.empty())
        assertThrows<ResourceNotFoundException> { goalService.getGoal(testUser, 99L) }
    }

    @Test
    fun `getGoals - returns all goals for user`() {
        val goal1 = Goal(id = 1L, goalName = "Goal 1", targetAmount = BigDecimal("1000"), targetDate = futureDate, startDate = startDate, user = testUser)
        val goal2 = Goal(id = 2L, goalName = "Goal 2", targetAmount = BigDecimal("2000"), targetDate = futureDate.plusMonths(1), startDate = startDate, user = testUser)

        whenever(goalRepository.findByUserOrderByTargetDateAsc(testUser)).thenReturn(listOf(goal1, goal2))
        whenever(transactionRepository.findByUserSinceDate(eq(testUser), any())).thenReturn(emptyList())

        val result = goalService.getGoals(testUser)

        assertEquals(2, result.size)
    }

    @Test
    fun `updateGoal - success updates fields`() {
        val goal = Goal(id = 1L, goalName = "Old Name", targetAmount = BigDecimal("1000"), targetDate = futureDate, startDate = startDate, user = testUser)
        val request = UpdateGoalRequest(goalName = "New Name", targetAmount = BigDecimal("2000"), targetDate = futureDate.plusMonths(1))

        whenever(goalRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.of(goal))
        whenever(goalRepository.save(any())).thenReturn(goal)
        whenever(transactionRepository.findByUserSinceDate(eq(testUser), any())).thenReturn(emptyList())

        val result = goalService.updateGoal(testUser, 1L, request)

        assertEquals("New Name", goal.goalName)
        assertEquals(BigDecimal("2000"), goal.targetAmount)
    }

    @Test
    fun `updateGoal - throws BadRequestException for past targetDate`() {
        val goal = Goal(id = 1L, goalName = "Goal", targetAmount = BigDecimal("1000"), targetDate = futureDate, startDate = startDate, user = testUser)
        val request = UpdateGoalRequest(goalName = "Goal", targetAmount = BigDecimal("1000"), targetDate = LocalDate.now().minusDays(1))

        whenever(goalRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.of(goal))

        assertThrows<BadRequestException> { goalService.updateGoal(testUser, 1L, request) }
    }

    @Test
    fun `deleteGoal - success deletes goal`() {
        val goal = Goal(id = 1L, goalName = "Goal", targetAmount = BigDecimal("1000"), targetDate = futureDate, user = testUser)
        whenever(goalRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.of(goal))

        goalService.deleteGoal(testUser, 1L)

        verify(goalRepository).delete(goal)
    }

    @Test
    fun `deleteGoal - throws ResourceNotFoundException when not found`() {
        whenever(goalRepository.findByIdAndUser(99L, testUser)).thenReturn(Optional.empty())
        assertThrows<ResourceNotFoundException> { goalService.deleteGoal(testUser, 99L) }
    }
}
