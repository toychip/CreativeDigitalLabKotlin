package com.chat.application.user

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface UserRepository : JpaRepository<UserEntity, String> {

    fun findByUserId(userId: String): Optional<UserEntity>

    fun existsByUserId(userId: String): Boolean
}
