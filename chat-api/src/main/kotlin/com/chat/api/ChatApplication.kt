package com.chat.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.chat"])
class ChatApplication

fun main(args: Array<String>) {
    runApplication<ChatApplication>(*args)
}
