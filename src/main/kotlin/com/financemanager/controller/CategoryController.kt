package com.financemanager.controller

import com.financemanager.dto.CategoryResponse
import com.financemanager.dto.CreateCategoryRequest
import com.financemanager.dto.MessageResponse
import com.financemanager.service.AuthService
import com.financemanager.service.CategoryService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/categories")
class CategoryController(
    private val categoryService: CategoryService,
    private val authService: AuthService
) {

    @GetMapping
    fun getCategories(
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<List<CategoryResponse>> {
        val user = authService.findUserByUsername(userDetails.username)
        return ResponseEntity.ok(categoryService.getCategories(user))
    }

    @PostMapping
    fun createCategory(
        @AuthenticationPrincipal userDetails: UserDetails,
        @Valid @RequestBody request: CreateCategoryRequest
    ): ResponseEntity<CategoryResponse> {
        val user = authService.findUserByUsername(userDetails.username)
        val category = categoryService.createCategory(user, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(category)
    }

    @DeleteMapping("/{name}")
    fun deleteCategory(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable name: String
    ): ResponseEntity<MessageResponse> {
        val user = authService.findUserByUsername(userDetails.username)
        categoryService.deleteCategory(user, name)
        return ResponseEntity.ok(MessageResponse("Category '$name' deleted successfully"))
    }
}
