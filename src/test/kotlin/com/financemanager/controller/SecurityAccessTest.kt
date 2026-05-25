package com.financemanager.controller

import com.financemanager.config.SecurityConfig
import com.financemanager.exception.GlobalExceptionHandler
import com.financemanager.security.UserDetailsServiceImpl
import com.financemanager.service.AuthService
import com.financemanager.service.CategoryService
import com.financemanager.service.GoalService
import com.financemanager.service.ReportService
import com.financemanager.service.TransactionService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

/**
 * Verifies that all protected endpoints return 401 when no authentication is provided,
 * and that auth endpoints are publicly accessible.
 */
@WebMvcTest(
    controllers = [
        AuthController::class,
        CategoryController::class,
        TransactionController::class,
        GoalController::class,
        ReportController::class
    ]
)
@Import(SecurityConfig::class, GlobalExceptionHandler::class)
class SecurityAccessTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean private lateinit var authService: AuthService
    @MockBean private lateinit var categoryService: CategoryService
    @MockBean private lateinit var transactionService: TransactionService
    @MockBean private lateinit var goalService: GoalService
    @MockBean private lateinit var reportService: ReportService
    @MockBean private lateinit var userDetailsService: UserDetailsServiceImpl

    // ── Public endpoints should not require auth ────────────────────────

    @Test
    fun `POST register is publicly accessible`() {
        mockMvc.perform(
            post("/api/auth/register")
                .contentType("application/json")
                .content("{}")
        )
            // May return 400 (validation) but NOT 401 — endpoint is public
            .andExpect(status().`is`(org.hamcrest.Matchers.not(401)))
    }

    @Test
    fun `POST login is publicly accessible`() {
        mockMvc.perform(
            post("/api/auth/login")
                .contentType("application/json")
                .content("{}")
        )
            .andExpect(status().`is`(org.hamcrest.Matchers.not(401)))
    }

    // ── Protected endpoints should return 401 when unauthenticated ──────

    @Test
    fun `GET categories requires authentication`() {
        mockMvc.perform(get("/api/categories"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `POST categories requires authentication`() {
        mockMvc.perform(post("/api/categories").contentType("application/json").content("{}"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `DELETE categories requires authentication`() {
        mockMvc.perform(delete("/api/categories/Food"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `GET transactions requires authentication`() {
        mockMvc.perform(get("/api/transactions"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `POST transactions requires authentication`() {
        mockMvc.perform(post("/api/transactions").contentType("application/json").content("{}"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `PUT transactions requires authentication`() {
        mockMvc.perform(put("/api/transactions/1").contentType("application/json").content("{}"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `DELETE transactions requires authentication`() {
        mockMvc.perform(delete("/api/transactions/1"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `GET goals requires authentication`() {
        mockMvc.perform(get("/api/goals"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `GET goal by id requires authentication`() {
        mockMvc.perform(get("/api/goals/1"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `POST goals requires authentication`() {
        mockMvc.perform(post("/api/goals").contentType("application/json").content("{}"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `PUT goals requires authentication`() {
        mockMvc.perform(put("/api/goals/1").contentType("application/json").content("{}"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `DELETE goals requires authentication`() {
        mockMvc.perform(delete("/api/goals/1"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `GET monthly report requires authentication`() {
        mockMvc.perform(get("/api/reports/monthly/2024/3"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `GET yearly report requires authentication`() {
        mockMvc.perform(get("/api/reports/yearly/2024"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `POST logout requires authentication`() {
        mockMvc.perform(post("/api/auth/logout"))
            .andExpect(status().isUnauthorized)
    }

    private fun status() = org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
}

fun org.springframework.test.web.servlet.result.StatusResultMatchers.isNot(status: Int) =
    org.hamcrest.Matchers.not(org.hamcrest.Matchers.equalTo(status)).let {
        org.springframework.test.web.servlet.ResultMatcher { result ->
            val actual = result.response.status
            if (actual == status) throw AssertionError("Expected status NOT to be $status but was $actual")
        }
    }
