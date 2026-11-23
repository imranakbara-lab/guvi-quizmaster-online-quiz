package com.quizmaster.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end user workflow through the real application (H2 database, real security, real services):
 * admin creates a quiz -> participant registers, takes it, gets scored -> sees history -> admin sees result.
 */
@SpringBootTest(properties = {"app.seed-sample-quizzes=false", "spring.datasource.url=jdbc:h2:mem:e2e;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc
class QuizFlowEndToEndTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;

    private JsonNode call(MockHttpServletRequestBuilder req, String token, Object body, int expectedStatus) throws Exception {
        if (token != null) req.header("Authorization", "Bearer " + token);
        if (body != null) req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        String res = mvc.perform(req).andExpect(status().is(expectedStatus)).andReturn().getResponse().getContentAsString();
        return res.isBlank() ? null : json.readTree(res);
    }

    @Test
    void fullQuizWorkflow() throws Exception {
        // 1. Admin logs in (seeded account) and builds a quiz
        String admin = call(post("/api/auth/login"), null,
                java.util.Map.of("email", "admin@quizmaster.com", "password", "Admin@123"), 200).get("token").asText();

        long quizId = call(post("/api/admin/quizzes"), admin,
                java.util.Map.of("title", "E2E Quiz", "description", "flow", "timeLimitMinutes", 5, "published", false), 201)
                .get("id").asLong();

        JsonNode q1 = call(post("/api/admin/quizzes/" + quizId + "/questions"), admin, java.util.Map.of("text", "Capital of India?",
                "options", java.util.List.of(java.util.Map.of("text", "New Delhi", "correct", true), java.util.Map.of("text", "Mumbai", "correct", false))), 201);
        JsonNode q2 = call(post("/api/admin/quizzes/" + quizId + "/questions"), admin, java.util.Map.of("text", "Even numbers?",
                "options", java.util.List.of(java.util.Map.of("text", "2", "correct", true), java.util.Map.of("text", "3", "correct", false),
                        java.util.Map.of("text", "4", "correct", true))), 201);

        // Draft quiz is not listed for participants yet
        assertThat(call(get("/api/quizzes"), null, null, 200)).isEmpty();
        call(put("/api/admin/quizzes/" + quizId), admin,
                java.util.Map.of("title", "E2E Quiz", "description", "flow", "timeLimitMinutes", 5, "published", true), 200);
        assertThat(call(get("/api/quizzes"), null, null, 200).get(0).get("title").asText()).isEqualTo("E2E Quiz");

        // 2. Participant registers and cannot use admin APIs
        String participant = call(post("/api/auth/register"), null, java.util.Map.of("name", "Priya",
                "email", "priya@mail.com", "password", "secret123"), 201).get("token").asText();
        call(get("/api/admin/quizzes"), participant, null, 403);

        // 3. Participant starts the quiz: no correct flags leak, timer is running
        JsonNode start = call(post("/api/quizzes/" + quizId + "/start"), participant, null, 200);
        assertThat(start.get("remainingSeconds").asLong()).isBetween(290L, 300L);
        assertThat(start.toString()).doesNotContain("\"correct\"");
        long attemptId = start.get("attemptId").asLong();

        // Starting again resumes the same attempt
        assertThat(call(post("/api/quizzes/" + quizId + "/start"), participant, null, 200).get("attemptId").asLong()).isEqualTo(attemptId);

        // 4. Submit: Q1 right, Q2 only half selected (wrong)
        long delhi = q1.get("options").get(0).get("id").asLong();
        long two = q2.get("options").get(0).get("id").asLong();
        JsonNode result = call(post("/api/attempts/" + attemptId + "/submit"), participant, java.util.Map.of("answers", java.util.List.of(
                java.util.Map.of("questionId", q1.get("id").asLong(), "selectedOptionIds", java.util.List.of(delhi)),
                java.util.Map.of("questionId", q2.get("id").asLong(), "selectedOptionIds", java.util.List.of(two)))), 200);
        assertThat(result.get("summary").get("score").asInt()).isEqualTo(1);
        assertThat(result.get("summary").get("percentage").asDouble()).isEqualTo(50.0);
        assertThat(result.get("review")).hasSize(2);

        // Cannot submit twice
        call(post("/api/attempts/" + attemptId + "/submit"), participant, java.util.Map.of("answers", java.util.List.of()), 400);

        // 5. History for participant and results for admin
        assertThat(call(get("/api/attempts/my"), participant, null, 200)).hasSize(1);
        assertThat(call(get("/api/attempts/" + attemptId), participant, null, 200).get("summary").get("quizTitle").asText()).isEqualTo("E2E Quiz");
        JsonNode adminResults = call(get("/api/admin/results").param("quizId", String.valueOf(quizId)), admin, null, 200);
        assertThat(adminResults.get(0).get("userEmail").asText()).isEqualTo("priya@mail.com");

        // 6. Password reset request always answers the same
        assertThat(call(post("/api/auth/forgot-password"), null, java.util.Map.of("email", "priya@mail.com"), 200)
                .get("message").asText()).contains("reset link");
    }
}
