package com.chat.application.user

import com.chat.domain.exception.CdlException
import com.chat.domain.exception.ExceptionCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserServiceImpl(
    private val userRepository: UserRepository
) : UserService {

    @Transactional
    override fun createUser(userId: String, username: String): UserCreateResponse {
        if (userRepository.existsByUserId(userId)) {
            throw CdlException(ExceptionCode.USER_ID_ALREADY_TAKEN)
        }
        val user = userRepository.save(UserEntity.create(userId, username))
        return UserCreateResponse.from(user)
    }
}
