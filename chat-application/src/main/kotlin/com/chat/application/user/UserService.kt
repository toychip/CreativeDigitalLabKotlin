package com.chat.application.user

interface UserService {

    fun createUser(userId: String, username: String): UserCreateResponse
}
