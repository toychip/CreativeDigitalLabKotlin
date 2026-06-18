package com.chat.application.user

data class UserCreateResponse(
    val userId: String,
    val username: String
) {
    companion object {
        fun from(user: UserEntity): UserCreateResponse =
            UserCreateResponse(user.userId, user.username)
    }
}
