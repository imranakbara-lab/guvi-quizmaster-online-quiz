package com.quizmaster.controller;

import com.quizmaster.dto.AttemptResult;
import com.quizmaster.dto.AttemptSummary;
import com.quizmaster.dto.SubmitRequest;
import com.quizmaster.security.UserPrincipal;
import com.quizmaster.service.AttemptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attempts")
@Tag(name = "Attempts", description = "Submit answers and see results")
public class AttemptController {

    private final AttemptService attemptService;

    public AttemptController(AttemptService attemptService) {
        this.attemptService = attemptService;
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit answers - graded instantly (also used for auto-submit when time runs out)")
    public AttemptResult submit(@PathVariable Long id, @Valid @RequestBody SubmitRequest request,
                                @AuthenticationPrincipal UserPrincipal me) {
        return attemptService.submit(id, request, me);
    }

    @GetMapping("/my")
    @Operation(summary = "My past results")
    public List<AttemptSummary> myResults(@AuthenticationPrincipal UserPrincipal me) {
        return attemptService.myResults(me);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detailed result with answer review")
    public AttemptResult result(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal me) {
        return attemptService.getResult(id, me);
    }
}
