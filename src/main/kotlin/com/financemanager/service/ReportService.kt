package com.financemanager.service

import com.financemanager.dto.*
import com.financemanager.entity.CategoryType
import com.financemanager.entity.Transaction
import com.financemanager.entity.User
import com.financemanager.exception.BadRequestException
import com.financemanager.repository.TransactionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class ReportService(
    private val transactionRepository: TransactionRepository
) {

    @Transactional(readOnly = true)
    fun getMonthlyReport(user: User, year: Int, month: Int): MonthlyReportResponse {
        validateYearMonth(year, month)
        val transactions = transactionRepository.findByUserAndYearAndMonth(user, year, month)
        return buildMonthlyReport(transactions, year, month)
    }

    @Transactional(readOnly = true)
    fun getYearlyReport(user: User, year: Int): YearlyReportResponse {
        validateYear(year)
        val transactions = transactionRepository.findByUserAndYear(user, year)
        return buildYearlyReport(transactions, year)
    }

    private fun buildMonthlyReport(
        transactions: List<Transaction>,
        year: Int,
        month: Int
    ): MonthlyReportResponse {
        val incomeTransactions = transactions.filter { it.category.type == CategoryType.INCOME }
        val expenseTransactions = transactions.filter { it.category.type == CategoryType.EXPENSE }

        val totalIncome = incomeTransactions.sumOf { it.amount }.setScale(2, RoundingMode.HALF_UP)
        val totalExpenses = expenseTransactions.sumOf { it.amount }.setScale(2, RoundingMode.HALF_UP)
        val netSavings = (totalIncome - totalExpenses).setScale(2, RoundingMode.HALF_UP)

        val incomeByCategory = incomeTransactions
            .groupBy { it.category }
            .map { (cat, txns) ->
                CategorySummary(
                    categoryId = cat.id,
                    categoryName = cat.name,
                    total = txns.sumOf { it.amount }.setScale(2, RoundingMode.HALF_UP)
                )
            }
            .sortedByDescending { it.total }

        val expensesByCategory = expenseTransactions
            .groupBy { it.category }
            .map { (cat, txns) ->
                CategorySummary(
                    categoryId = cat.id,
                    categoryName = cat.name,
                    total = txns.sumOf { it.amount }.setScale(2, RoundingMode.HALF_UP)
                )
            }
            .sortedByDescending { it.total }

        return MonthlyReportResponse(
            year = year,
            month = month,
            totalIncome = totalIncome,
            totalExpenses = totalExpenses,
            netSavings = netSavings,
            incomeByCategory = incomeByCategory,
            expensesByCategory = expensesByCategory
        )
    }

    private fun buildYearlyReport(
        transactions: List<Transaction>,
        year: Int
    ): YearlyReportResponse {
        val incomeTransactions = transactions.filter { it.category.type == CategoryType.INCOME }
        val expenseTransactions = transactions.filter { it.category.type == CategoryType.EXPENSE }

        val totalIncome = incomeTransactions.sumOf { it.amount }.setScale(2, RoundingMode.HALF_UP)
        val totalExpenses = expenseTransactions.sumOf { it.amount }.setScale(2, RoundingMode.HALF_UP)
        val netSavings = (totalIncome - totalExpenses).setScale(2, RoundingMode.HALF_UP)

        val monthlyBreakdown = (1..12).map { month ->
            val monthTransactions = transactions.filter { it.date.monthValue == month }
            val mIncome = monthTransactions
                .filter { it.category.type == CategoryType.INCOME }
                .sumOf { it.amount }.setScale(2, RoundingMode.HALF_UP)
            val mExpenses = monthTransactions
                .filter { it.category.type == CategoryType.EXPENSE }
                .sumOf { it.amount }.setScale(2, RoundingMode.HALF_UP)
            MonthlyBreakdown(
                month = month,
                totalIncome = mIncome,
                totalExpenses = mExpenses,
                netSavings = (mIncome - mExpenses).setScale(2, RoundingMode.HALF_UP)
            )
        }

        val incomeByCategory = incomeTransactions
            .groupBy { it.category }
            .map { (cat, txns) ->
                CategorySummary(
                    categoryId = cat.id,
                    categoryName = cat.name,
                    total = txns.sumOf { it.amount }.setScale(2, RoundingMode.HALF_UP)
                )
            }
            .sortedByDescending { it.total }

        val expensesByCategory = expenseTransactions
            .groupBy { it.category }
            .map { (cat, txns) ->
                CategorySummary(
                    categoryId = cat.id,
                    categoryName = cat.name,
                    total = txns.sumOf { it.amount }.setScale(2, RoundingMode.HALF_UP)
                )
            }
            .sortedByDescending { it.total }

        return YearlyReportResponse(
            year = year,
            totalIncome = totalIncome,
            totalExpenses = totalExpenses,
            netSavings = netSavings,
            monthlyBreakdown = monthlyBreakdown,
            incomeByCategory = incomeByCategory,
            expensesByCategory = expensesByCategory
        )
    }

    private fun validateYearMonth(year: Int, month: Int) {
        if (year < 2000 || year > 2100) throw BadRequestException("Year must be between 2000 and 2100")
        if (month < 1 || month > 12) throw BadRequestException("Month must be between 1 and 12")
    }

    private fun validateYear(year: Int) {
        if (year < 2000 || year > 2100) throw BadRequestException("Year must be between 2000 and 2100")
    }
}
