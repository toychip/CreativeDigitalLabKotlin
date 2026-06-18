package com.chat.api.exception

import com.chat.domain.exception.CdlException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.resource.NoResourceFoundException

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(CdlException::class)
    fun handleCdlException(e: CdlException): ResponseEntity<ExceptionResponse> {
        val status = e.statusCode() ?: INTERNAL_SERVER_ERROR
        if (status >= INTERNAL_SERVER_ERROR) {
            log.error("CdlException ({}) code={}", status, e.code, e)
        } else {
            log.warn("CdlException ({}) code={}, message={}", status, e.code, e.message)
        }
        return ResponseEntity.status(status)
            .body(ExceptionResponse.of(e.defaultMessage(), e.detail))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException): ResponseEntity<ExceptionResponse> {
        val cause = e.bindingResult.fieldErrors.joinToString(", ") { fieldError ->
            "${fieldError.field}: ${fieldError.defaultMessage}"
        }
        log.warn("Validation 실패 - {}", cause)
        return ResponseEntity.badRequest()
            .body(ExceptionResponse.of("요청 형식이 잘못되었습니다.", cause))
    }

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResource(e: NoResourceFoundException): ResponseEntity<ExceptionResponse> {
        log.warn("존재하지 않는 경로 요청 - {}", e.resourcePath)
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ExceptionResponse.of("존재하지 않는 API 입니다. API 명세서(/swagger-ui/index.html)를 참고해주세요.", e.resourcePath))
    }

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ExceptionResponse> {
        log.error("예기치 못한 에러 발생", e)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ExceptionResponse.of("예기치 못한 에러가 발생했습니다.", null))
    }

    companion object {
        private const val INTERNAL_SERVER_ERROR = 500
        private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    }
}
