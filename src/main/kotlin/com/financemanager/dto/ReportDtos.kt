package com.financemanager.dto

import java.math.BigDecimal

data class CategorySummary(
    val categoryId: Long,
    val categoryName: String,
    val total: BigDecimal
)

data class MonthlyReportResponse(
    val year: Int,
    val month: Int,
    val totalIncome: BigDecimal,
    val totalExpenses: BigDecimal,
    val netSavings: BigDecimal,
    val incomeByCategory: List<CategorySummary>,
    val expensesByCategory: List<CategorySummary>
)

data class MonthlyBreakdown(
    val month: Int,
    val totalIncome: BigDecimal,
    val totalExpenses: BigDecimal,
    val netSavings: BigDecimal
)

data class YearlyReportResponse(
    val year: Int,
    val totalIncome: BigDecimal,
    val totalExpenses: BigDecimal,
    val netSavings: BigDecimal,
    val monthlyBreakdown: List<MonthlyBreakdown>,
    val incomeByCategory: List<CategorySummary>,
    val expensesByCategory: List<CategorySummary>
)
