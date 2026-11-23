package com.quizmaster.controller;

import com.quizmaster.config.SecurityConfig;
import com.quizmaster.dto.*;
import com.quizmaster.exception.ForbiddenException;
import com.quizmaster.security.CustomUserDetailsService;
import com.quizmaster.security.JwtService;
import com.quizmaster.security.UserPrincipal;
import com.quizmaster.service.AttemptService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
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

@WebMvcTest(AttemptController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:5173")
class AttemptControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private AttemptService attemptService;
    @MockBean private JwtService jwtService;
    @MockBean private CustomUserDetailsService userDetailsService;

    private final UserPrincipal participant = new UserPrincipal(5L, "p@mail.com", "x", "PARTICIPANT");

    private AttemptSummary summary() {
        return new AttemptSummary(50L, 1L, "Java Basics", "P", "p@mail.com", 1, 2, 50.0, false,
                LocalDateTime.now().minusMinutes(3), LocalDateTime.now());
    }

    @Test
    void submit_returnsScore() throws Exception {
        when(attemptService.submit(eq(50L), any(SubmitRequest.class), any(UserPrincipal.class)))
                .thenReturn(new AttemptResult(summary(), List.of()));

        mockMvc.perform(post("/api/attempts/50/submit").with(user(participant))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answers\":[{\"questionId\":10,\"selectedOptionIds\":[100]}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.score").value(1))
                .andExpect(jsonPath("$.summary.percentage").value(50.0));
    }

    @Test
    void submit_answerWithoutQuestionId_returns400() throws Exception {
        mockMvc.perform(post("/api/attempts/50/submit").with(user(participant))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answers\":[{\"selectedOptionIds\":[100]}]}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(attemptService);
    }

    @Test
    void myResults_listsAttempts() throws Exception {
        when(attemptService.myResults(any(UserPrincipal.class))).thenReturn(List.of(summary()));

        mockMvc.perform(get("/api/attempts/my").with(user(participant)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].quizTitle").value("Java Basics"));
    }

    @Test
    void result_ofOtherUser_returns403() throws Exception {
        when(attemptService.getResult(eq(51L), any(UserPrincipal.class))).thenThrow(new ForbiddenException("This is not your attempt"));

        mockMvc.perform(get("/api/attempts/51").with(user(participant))).andExpect(status().isForbidden());
    }

    @Test
    void myResults_requiresLogin() throws Exception {
        mockMvc.perform(get("/api/attempts/my")).andExpect(status().isUnauthorized());
    }
}
