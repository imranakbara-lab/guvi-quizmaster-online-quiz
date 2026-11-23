package com.quizmaster.controller;

import com.quizmaster.config.SecurityConfig;
import com.quizmaster.dto.*;
import com.quizmaster.security.CustomUserDetailsService;
import com.quizmaster.security.JwtService;
import com.quizmaster.security.UserPrincipal;
import com.quizmaster.service.AttemptService;
import com.quizmaster.service.QuizService;
import com.quizmaster.service.UserService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:5173")
class AdminControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private QuizService quizService;
    @MockBean private AttemptService attemptService;
    @MockBean private UserService userService;
    @MockBean private JwtService jwtService;
    @MockBean private CustomUserDetailsService userDetailsService;

    private final UserPrincipal admin = new UserPrincipal(1L, "admin@mail.com", "x", "ADMIN");
    private final UserPrincipal participant = new UserPrincipal(5L, "p@mail.com", "x", "PARTICIPANT");

    @Test
    void createQuiz_asAdmin_returns201() throws Exception {
        when(quizService.create(any(QuizRequest.class)))
                .thenReturn(new QuizDetail(3L, "Maths", "d", 10, false, LocalDateTime.now(), List.of()));

        mockMvc.perform(post("/api/admin/quizzes").with(user(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Maths\",\"description\":\"d\",\"timeLimitMinutes\":10,\"published\":false}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3));
    }

    @Test
    void createQuiz_invalidTimeLimit_returns400() throws Exception {
        mockMvc.perform(post("/api/admin/quizzes").with(user(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"timeLimitMinutes\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").exists())
                .andExpect(jsonPath("$.fieldErrors.timeLimitMinutes").exists());
    }

    @Test
    void createQuiz_asParticipant_returns403() throws Exception {
        mockMvc.perform(post("/api/admin/quizzes").with(user(participant)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Maths\",\"timeLimitMinutes\":10}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(quizService);
    }

    @Test
    void addQuestion_needsTwoOptions() throws Exception {
        mockMvc.perform(post("/api/admin/quizzes/3/questions").with(user(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Q?\",\"options\":[{\"text\":\"only\",\"correct\":true}]}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(quizService);
    }

    @Test
    void addQuestion_ok() throws Exception {
        when(quizService.addQuestion(eq(3L), any(QuestionRequest.class))).thenReturn(new QuestionAdminView(30L, "Q?", 1,
                List.of(new OptionAdminView(300L, "A", true), new OptionAdminView(301L, "B", false))));

        mockMvc.perform(post("/api/admin/quizzes/3/questions").with(user(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Q?\",\"options\":[{\"text\":\"A\",\"correct\":true},{\"text\":\"B\",\"correct\":false}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.options[0].correct").value(true));
    }

    @Test
    void deleteQuestion_returns204() throws Exception {
        mockMvc.perform(delete("/api/admin/questions/30").with(user(admin))).andExpect(status().isNoContent());
        verify(quizService).deleteQuestion(30L);
    }

    @Test
    void results_forQuiz() throws Exception {
        when(attemptService.allResults(3L)).thenReturn(List.of());
        mockMvc.perform(get("/api/admin/results").param("quizId", "3").with(user(admin))).andExpect(status().isOk());
        verify(attemptService).allResults(3L);
    }
}
