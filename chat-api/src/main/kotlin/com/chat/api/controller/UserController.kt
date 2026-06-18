package com.chat.api.controller

import com.chat.api.controller.docs.UserControllerDocs
import com.chat.api.dto.UserCreateRequest
import com.chat.application.user.UserCreateResponse
import com.chat.application.user.UserService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/users")
class UserController(
    private val userService: UserService
) : UserControllerDocs {

    @PostMapping
    override fun createUser(@RequestBody request: UserCreateRequest): UserCreateResponse =
        userService.createUser(request.userId, request.username)
}
