package com.chat.application.common

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

object JsonUtil {

    private val OBJECT_MAPPER: ObjectMapper = ObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

    fun toJson(value: Any): String =
        try {
            OBJECT_MAPPER.writeValueAsString(value)
        } catch (e: JsonProcessingException) {
            throw IllegalStateException("JSON 직렬화 실패: ${value.javaClass.simpleName}", e)
        }

    fun <T> fromJson(json: String, type: Class<T>): T =
        try {
            OBJECT_MAPPER.readValue(json, type)
        } catch (e: JsonProcessingException) {
            throw IllegalStateException("JSON 역직렬화 실패: ${type.simpleName}", e)
        }
}
