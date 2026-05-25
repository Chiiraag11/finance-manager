package com.financemanager.repository

import com.financemanager.entity.Category
import com.financemanager.entity.Transaction
import com.financemanager.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.Optional

@Repository
interface TransactionRepository : JpaRepository<Transaction, Long> {

    @Query("""
        SELECT t FROM Transaction t
        WHERE t.user = :user
          AND t.deleted = false
          AND (:startDate IS NULL OR t.date >= :startDate)
          AND (:endDate IS NULL OR t.date <= :endDate)
          AND (:categoryId IS NULL OR t.category.id = :categoryId)
        ORDER BY t.date DESC, t.id DESC
    """)
    fun findByUserWithFilters(
        @Param("user") user: User,
        @Param("startDate") startDate: LocalDate?,
        @Param("endDate") endDate: LocalDate?,
        @Param("categoryId") categoryId: Long?
    ): List<Transaction>

    fun findByIdAndUserAndDeletedFalse(id: Long, user: User): Optional<Transaction>

    fun existsByCategoryAndDeletedFalse(category: Category): Boolean

    @Query("""
        SELECT t FROM Transaction t
        WHERE t.user = :user
          AND t.deleted = false
          AND FUNCTION('YEAR', t.date) = :year
          AND FUNCTION('MONTH', t.date) = :month
        ORDER BY t.date DESC
    """)
    fun findByUserAndYearAndMonth(
        @Param("user") user: User,
        @Param("year") year: Int,
        @Param("month") month: Int
    ): List<Transaction>

    @Query("""
        SELECT t FROM Transaction t
        WHERE t.user = :user
          AND t.deleted = false
          AND FUNCTION('YEAR', t.date) = :year
        ORDER BY t.date DESC
    """)
    fun findByUserAndYear(
        @Param("user") user: User,
        @Param("year") year: Int
    ): List<Transaction>

    @Query("""
        SELECT t FROM Transaction t
        WHERE t.user = :user
          AND t.deleted = false
          AND t.date >= :startDate
        ORDER BY t.date DESC
    """)
    fun findByUserSinceDate(
        @Param("user") user: User,
        @Param("startDate") startDate: LocalDate
    ): List<Transaction>
}
