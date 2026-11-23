package com.quizmaster.controller;

import com.quizmaster.dto.*;
import com.quizmaster.security.UserPrincipal;
import com.quizmaster.service.AttemptService;
import com.quizmaster.service.QuizService;
import com.quizmaster.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Quiz & question management, results and users (ADMIN only)")
public class AdminController {

    private final QuizService quizService;
    private final AttemptService attemptService;
    private final UserService userService;

    public AdminController(QuizService quizService, AttemptService attemptService, UserService userService) {
        this.quizService = quizService;
        this.attemptService = attemptService;
        this.userService = userService;
    }

    // ----- Quizzes -----
    @GetMapping("/quizzes")
    @Operation(summary = "All quizzes (published and drafts)")
    public List<QuizSummary> quizzes() { return quizService.allQuizzes(); }

    @GetMapping("/quizzes/{id}")
    @Operation(summary = "Quiz with questions and correct answers")
    public QuizDetail quiz(@PathVariable Long id) { return quizService.getDetail(id); }

    @PostMapping("/quizzes")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a quiz")
    public QuizDetail createQuiz(@Valid @RequestBody QuizRequest request) { return quizService.create(request); }

    @PutMapping("/quizzes/{id}")
    @Operation(summary = "Update quiz title, description, time limit or published flag")
    public QuizDetail updateQuiz(@PathVariable Long id, @Valid @RequestBody QuizRequest request) {
        return quizService.update(id, request);
    }

    @DeleteMapping("/quizzes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a quiz with its questions and attempts")
    public void deleteQuiz(@PathVariable Long id) { quizService.delete(id); }

    // ----- Questions -----
    @PostMapping("/quizzes/{quizId}/questions")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a multiple-choice question")
    public QuestionAdminView addQuestion(@PathVariable Long quizId, @Valid @RequestBody QuestionRequest request) {
        return quizService.addQuestion(quizId, request);
    }

    @PutMapping("/questions/{id}")
    @Operation(summary = "Update a question and its options")
    public QuestionAdminView updateQuestion(@PathVariable Long id, @Valid @RequestBody QuestionRequest request) {
        return quizService.updateQuestion(id, request);
    }

    @DeleteMapping("/questions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a question")
    public void deleteQuestion(@PathVariable Long id) { quizService.deleteQuestion(id); }

    // ----- Results -----
    @GetMapping("/results")
    @Operation(summary = "All submitted attempts, optionally for one quiz (leaderboard order)")
    public List<AttemptSummary> results(@RequestParam(required = false) Long quizId) {
        return attemptService.allResults(quizId);
    }

    // ----- Users -----
    @GetMapping("/users")
    @Operation(summary = "List all users")
    public List<UserResponse> users() { return userService.listAll(); }

    @PutMapping("/users/{id}")
    @Operation(summary = "Edit a user (name, email, phone, role)")
    public UserResponse updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return userService.update(id, request);
    }

    @DeleteMapping("/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a user and their attempts")
    public void deleteUser(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal me) {
        userService.delete(id, me.getId());
    }
}
