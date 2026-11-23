package com.quizmaster.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Emails are sent in the background so API responses stay fast. */
@Configuration
@EnableAsync
public class AsyncConfig {
}
