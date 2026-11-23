package com.quizmaster.controller;

import com.quizmaster.config.SecurityConfig;
import com.quizmaster.dto.*;
import com.quizmaster.exception.BadRequestException;
import com.quizmaster.security.CustomUserDetailsService;
import com.quizmaster.security.JwtService;
import com.quizmaster.security.UserPrincipal;
import com.quizmaster.service.AttemptService;
import com.quizmaster.service.QuizService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(QuizController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:5173")
class QuizControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private QuizService quizService;
    @MockBean private AttemptService attemptService;
    @MockBean private JwtService jwtService;
    @MockBean private CustomUserDetailsService userDetailsService;

    private final UserPrincipal participant = new UserPrincipal(5L, "p@mail.com", "x", "PARTICIPANT");

    @Test
    void listQuizzes_isPublic() throws Exception {
        when(quizService.availableQuizzes()).thenReturn(List.of(
                new QuizSummary(1L, "Java Basics", "d", 10, 5, true, LocalDateTime.now())));

        mockMvc.perform(get("/api/quizzes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Java Basics"))
                .andExpect(jsonPath("$[0].questionCount").value(5));
    }

    @Test
    void start_requiresLogin() throws Exception {
        mockMvc.perform(post("/api/quizzes/1/start")).andExpect(status().isUnauthorized());
        verifyNoInteractions(attemptService);
    }

    @Test
    void start_returnsQuestionsWithoutCorrectFlag() throws Exception {
        when(attemptService.start(eq(1L), any(UserPrincipal.class))).thenReturn(new AttemptStartResponse(50L, 1L,
                "Java Basics", "d", 10, 600,
                List.of(new QuestionView(10L, "Q1", false, List.of(new OptionView(100L, "A"), new OptionView(101L, "B"))))));

        mockMvc.perform(post("/api/quizzes/1/start").with(user(participant)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attemptId").value(50))
                .andExpect(jsonPath("$.remainingSeconds").value(600))
                .andExpect(jsonPath("$.questions[0].options[0].text").value("A"))
                .andExpect(jsonPath("$.questions[0].options[0].correct").doesNotExist());
    }

    @Test
    void start_unavailableQuiz_returns400() throws Exception {
        when(attemptService.start(eq(2L), any(UserPrincipal.class))).thenThrow(new BadRequestException("This quiz is not available"));

        mockMvc.perform(post("/api/quizzes/2/start").with(user(participant)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("This quiz is not available"));
    }
}
