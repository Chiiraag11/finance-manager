package com.financemanager.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(name = "goals")
class Goal(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, length = 200)
    var goalName: String,

    @Column(nullable = false, precision = 19, scale = 2)
    var targetAmount: BigDecimal,

    @Column(nullable = false)
    var targetDate: LocalDate,

    @Column(nullable = false)
    val startDate: LocalDate = LocalDate.now(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Goal) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
