package com.financemanager.controller

import com.financemanager.dto.CreateGoalRequest
import com.financemanager.dto.GoalResponse
import com.financemanager.dto.MessageResponse
import com.financemanager.dto.UpdateGoalRequest
import com.financemanager.service.AuthService
import com.financemanager.service.GoalService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/goals")
class GoalController(
    private val goalService: GoalService,
    private val authService: AuthService
) {

    @PostMapping
    fun createGoal(
        @AuthenticationPrincipal userDetails: UserDetails,
        @Valid @RequestBody request: CreateGoalRequest
    ): ResponseEntity<GoalResponse> {
        val user = authService.findUserByUsername(userDetails.username)
        val goal = goalService.createGoal(user, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(goal)
    }

    @GetMapping
    fun getGoals(
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<List<GoalResponse>> {
        val user = authService.findUserByUsername(userDetails.username)
        return ResponseEntity.ok(goalService.getGoals(user))
    }

    @GetMapping("/{id}")
    fun getGoal(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable id: Long
    ): ResponseEntity<GoalResponse> {
        val user = authService.findUserByUsername(userDetails.username)
        return ResponseEntity.ok(goalService.getGoal(user, id))
    }

    @PutMapping("/{id}")
    fun updateGoal(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateGoalRequest
    ): ResponseEntity<GoalResponse> {
        val user = authService.findUserByUsername(userDetails.username)
        val goal = goalService.updateGoal(user, id, request)
        return ResponseEntity.ok(goal)
    }

    @DeleteMapping("/{id}")
    fun deleteGoal(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable id: Long
    ): ResponseEntity<MessageResponse> {
        val user = authService.findUserByUsername(userDetails.username)
        goalService.deleteGoal(user, id)
        return ResponseEntity.ok(MessageResponse("Goal deleted successfully"))
    }
}
