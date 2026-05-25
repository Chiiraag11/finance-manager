package com.financemanager

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class FinanceManagerApplication

fun main(args: Array<String>) {
    runApplication<FinanceManagerApplication>(*args)
}
