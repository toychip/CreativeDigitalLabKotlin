package com.chat.domain.common

import com.fasterxml.uuid.Generators

/**
 * UUID v7 생성 유틸
 */
object IdGenerator {

    fun generate(): String =
        Generators.timeBasedEpochGenerator().generate().toString()
}
