package com.chat.application.config

import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Configuration
@EntityScan(basePackages = ["com.chat.application"])
@EnableJpaRepositories(basePackages = ["com.chat.application"])
@EnableJpaAuditing
class JpaConfig
