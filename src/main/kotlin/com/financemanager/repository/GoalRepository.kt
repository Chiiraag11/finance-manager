package com.financemanager.repository

import com.financemanager.entity.Goal
import com.financemanager.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface GoalRepository : JpaRepository<Goal, Long> {
    fun findByUserOrderByTargetDateAsc(user: User): List<Goal>
    fun findByIdAndUser(id: Long, user: User): Optional<Goal>
}
