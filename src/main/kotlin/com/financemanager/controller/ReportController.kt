package com.financemanager.controller

import com.financemanager.dto.MonthlyReportResponse
import com.financemanager.dto.YearlyReportResponse
import com.financemanager.service.AuthService
import com.financemanager.service.ReportService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/reports")
class ReportController(
    private val reportService: ReportService,
    private val authService: AuthService
) {

    @GetMapping("/monthly/{year}/{month}")
    fun getMonthlyReport(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable year: Int,
        @PathVariable month: Int
    ): ResponseEntity<MonthlyReportResponse> {
        val user = authService.findUserByUsername(userDetails.username)
        return ResponseEntity.ok(reportService.getMonthlyReport(user, year, month))
    }

    @GetMapping("/yearly/{year}")
    fun getYearlyReport(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable year: Int
    ): ResponseEntity<YearlyReportResponse> {
        val user = authService.findUserByUsername(userDetails.username)
        return ResponseEntity.ok(reportService.getYearlyReport(user, year))
    }
}
