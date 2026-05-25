package com.financemanager.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.financemanager.config.SecurityConfig
import com.financemanager.dto.CategoryResponse
import com.financemanager.dto.CreateCategoryRequest
import com.financemanager.entity.CategoryType
import com.financemanager.entity.User
import com.financemanager.exception.BadRequestException
import com.financemanager.exception.ConflictException
import com.financemanager.exception.DuplicateResourceException
import com.financemanager.exception.GlobalExceptionHandler
import com.financemanager.exception.ResourceNotFoundException
import com.financemanager.security.UserDetailsServiceImpl
import com.financemanager.service.AuthService
import com.financemanager.service.CategoryService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(CategoryController::class)
@Import(SecurityConfig::class, GlobalExceptionHandler::class)
class CategoryControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var categoryService: CategoryService

    @MockBean
    private lateinit var authService: AuthService

    @MockBean
    private lateinit var userDetailsService: UserDetailsServiceImpl

    private val testUser = User(id = 1L, username = "test@example.com", password = "hashed", fullName = "Test", phoneNumber = "123")

    private val sampleCategories = listOf(
        CategoryResponse(id = 1L, name = "Salary", type = CategoryType.INCOME, isDefault = true),
        CategoryResponse(id = 2L, name = "Food", type = CategoryType.EXPENSE, isDefault = true),
        CategoryResponse(id = 3L, name = "Freelance", type = CategoryType.INCOME, isDefault = false)
    )

    @Test
    fun `GET categories - returns 401 for unauthenticated`() {
        mockMvc.perform(get("/api/categories"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "test@example.com")
    fun `GET categories - returns list for authenticated user`() {
        whenever(authService.findUserByUsername("test@example.com")).thenReturn(testUser)
        whenever(categoryService.getCategories(testUser)).thenReturn(sampleCategories)

        mockMvc.perform(get("/api/categories"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].name").value("Salary"))
            .andExpect(jsonPath("$[0].isDefault").value(true))
            .andExpect(jsonPath("$[2].name").value("Freelance"))
            .andExpect(jsonPath("$[2].isDefault").value(false))
    }

    @Test
    @WithMockUser(username = "test@example.com")
    fun `POST categories - returns 201 on success`() {
        val request = CreateCategoryRequest(name = "Freelance", type = CategoryType.INCOME)
        val created = CategoryResponse(id = 10L, name = "Freelance", type = CategoryType.INCOME, isDefault = false)

        whenever(authService.findUserByUsername("test@example.com")).thenReturn(testUser)
        whenever(categoryService.createCategory(testUser, request)).thenReturn(created)

        mockMvc.perform(
            post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("Freelance"))
            .andExpect(jsonPath("$.type").value("INCOME"))
            .andExpect(jsonPath("$.isDefault").value(false))
    }

    @Test
    @WithMockUser(username = "test@example.com")
    fun `POST categories - returns 409 for duplicate name`() {
        val request = CreateCategoryRequest(name = "Salary", type = CategoryType.INCOME)

        whenever(authService.findUserByUsername("test@example.com")).thenReturn(testUser)
        whenever(categoryService.createCategory(testUser, request))
            .thenThrow(DuplicateResourceException("Category already exists"))

        mockMvc.perform(
            post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isConflict)
    }

    @Test
    @WithMockUser(username = "test@example.com")
    fun `POST categories - returns 400 for missing name`() {
        val request = mapOf("type" to "INCOME")

        mockMvc.perform(
            post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @WithMockUser(username = "test@example.com")
    fun `DELETE categories - returns 200 on success`() {
        whenever(authService.findUserByUsername("test@example.com")).thenReturn(testUser)
        doNothing().whenever(categoryService).deleteCategory(testUser, "Freelance")

        mockMvc.perform(delete("/api/categories/Freelance"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").exists())
    }

    @Test
    @WithMockUser(username = "test@example.com")
    fun `DELETE categories - returns 400 when deleting default category`() {
        whenever(authService.findUserByUsername("test@example.com")).thenReturn(testUser)
        whenever(categoryService.deleteCategory(testUser, "Salary"))
            .thenThrow(BadRequestException("Default category cannot be deleted"))

        mockMvc.perform(delete("/api/categories/Salary"))
            .andExpect(status().isBadRequest)
    }

    @Test
    @WithMockUser(username = "test@example.com")
    fun `DELETE categories - returns 404 when category not found`() {
        whenever(authService.findUserByUsername("test@example.com")).thenReturn(testUser)
        whenever(categoryService.deleteCategory(testUser, "Unknown"))
            .thenThrow(ResourceNotFoundException("Category not found"))

        mockMvc.perform(delete("/api/categories/Unknown"))
            .andExpect(status().isNotFound)
    }

    @Test
    @WithMockUser(username = "test@example.com")
    fun `DELETE categories - returns 409 when category has transactions`() {
        whenever(authService.findUserByUsername("test@example.com")).thenReturn(testUser)
        whenever(categoryService.deleteCategory(testUser, "Food"))
            .thenThrow(ConflictException("Category has transactions"))

        mockMvc.perform(delete("/api/categories/Food"))
            .andExpect(status().isConflict)
    }

    @Test
    fun `DELETE categories - returns 401 for unauthenticated`() {
        mockMvc.perform(delete("/api/categories/Food"))
            .andExpect(status().isUnauthorized)
    }
}
