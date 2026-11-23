package com.quizmaster.controller;

import com.quizmaster.dto.AttemptStartResponse;
import com.quizmaster.dto.QuizSummary;
import com.quizmaster.security.UserPrincipal;
import com.quizmaster.service.AttemptService;
import com.quizmaster.service.QuizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quizzes")
@Tag(name = "Quizzes", description = "Browse and start quizzes")
public class QuizController {

    private final QuizService quizService;
    private final AttemptService attemptService;

    public QuizController(QuizService quizService, AttemptService attemptService) {
        this.quizService = quizService;
        this.attemptService = attemptService;
    }

    @GetMapping
    @Operation(summary = "Published quizzes available to take")
    public List<QuizSummary> available() {
        return quizService.availableQuizzes();
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "Start (or resume) a quiz - returns questions without answers and the remaining time")
    public AttemptStartResponse start(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal me) {
        return attemptService.start(id, me);
    }
}
