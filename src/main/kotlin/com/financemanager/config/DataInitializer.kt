package com.financemanager.config

import com.financemanager.entity.Category
import com.financemanager.entity.CategoryType
import com.financemanager.repository.CategoryRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class DataInitializer(
    private val categoryRepository: CategoryRepository
) : ApplicationRunner {

    private val logger = LoggerFactory.getLogger(DataInitializer::class.java)

    companion object {
        val DEFAULT_INCOME_CATEGORIES = listOf("Salary")
        val DEFAULT_EXPENSE_CATEGORIES = listOf(
            "Food", "Rent", "Transportation", "Entertainment", "Healthcare", "Utilities"
        )
    }

    @Transactional
    override fun run(args: ApplicationArguments) {
        logger.info("Initializing default categories...")

        DEFAULT_INCOME_CATEGORIES.forEach { name ->
            if (categoryRepository.findByNameAndUserIsNull(name).isEmpty) {
                categoryRepository.save(
                    Category(name = name, type = CategoryType.INCOME, isDefault = true, user = null)
                )
                logger.debug("Created default income category: $name")
            }
        }

        DEFAULT_EXPENSE_CATEGORIES.forEach { name ->
            if (categoryRepository.findByNameAndUserIsNull(name).isEmpty) {
                categoryRepository.save(
                    Category(name = name, type = CategoryType.EXPENSE, isDefault = true, user = null)
                )
                logger.debug("Created default expense category: $name")
            }
        }

        logger.info("Default categories initialized successfully.")
    }
}
