package com.financemanager.entity

import jakarta.persistence.*

@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(unique = true, nullable = false, length = 255)
    var username: String,

    @Column(nullable = false)
    var password: String,

    @Column(nullable = false, length = 100)
    var fullName: String,

    @Column(nullable = false, length = 20)
    var phoneNumber: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is User) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
