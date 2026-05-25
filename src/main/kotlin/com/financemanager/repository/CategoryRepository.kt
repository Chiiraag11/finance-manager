package com.financemanager.repository

import com.financemanager.entity.Category
import com.financemanager.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface CategoryRepository : JpaRepository<Category, Long> {

    @Query("SELECT c FROM Category c WHERE c.user IS NULL OR c.user = :user ORDER BY c.isDefault DESC, c.name ASC")
    fun findAllForUser(@Param("user") user: User): List<Category>

    @Query("SELECT c FROM Category c WHERE c.id = :id AND (c.user IS NULL OR c.user = :user)")
    fun findByIdAndAccessibleByUser(@Param("id") id: Long, @Param("user") user: User): Optional<Category>

    fun findByNameAndUser(name: String, user: User): Optional<Category>

    fun findByNameAndUserIsNull(name: String): Optional<Category>

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN TRUE ELSE FALSE END FROM Category c WHERE LOWER(c.name) = LOWER(:name) AND (c.user = :user OR c.user IS NULL)")
    fun existsByNameIgnoreCaseForUser(@Param("name") name: String, @Param("user") user: User): Boolean

}
