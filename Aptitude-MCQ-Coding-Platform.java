package com.prep.platform.controller;

import com.prep.platform.dto.CodeRunRequest;
import com.prep.platform.dto.CodeRunResponse;
import com.prep.platform.dto.CodingProblemDTO;
import com.prep.platform.model.CodingSubmission;
import com.prep.platform.model.User;
import com.prep.platform.model.enums.ProgrammingLanguage;
import com.prep.platform.repository.CodingSubmissionRepository;
import com.prep.platform.service.AuthService;
import com.prep.platform.service.CodingProblemService;
import com.prep.platform.service.EvaluationService;
import com.prep.platform.service.execution.CodeExecutionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/coding")
public class CodingController {

    private final CodingProblemService problemService;
    private final EvaluationService evaluationService;
    private final AuthService authService;
    private final CodeExecutionService codeExecutionService;
    private final CodingSubmissionRepository submissionRepository;

    public CodingController(
            CodingProblemService problemService,
            EvaluationService evaluationService,
            AuthService authService,
            CodeExecutionService codeExecutionService,
            CodingSubmissionRepository submissionRepository
    ) {
        this.problemService = problemService;
        this.evaluationService = evaluationService;
        this.authService = authService;
        this.codeExecutionService = codeExecutionService;
        this.submissionRepository = submissionRepository;
    }

    @GetMapping("/problems")
    public ResponseEntity<List<CodingProblemDTO>> getAllProblems() {
        return ResponseEntity.ok(problemService.getAllProblems(true));
    }

    @PostMapping("/problems")
    public ResponseEntity<?> createProblem(@RequestBody CodingProblemDTO dto) {
        return ResponseEntity.ok(problemService.createProblem(dto));
    }

    @GetMapping("/problems/{id}")
    public ResponseEntity<CodingProblemDTO> getProblemById(@PathVariable Long id) {
        return ResponseEntity.ok(problemService.getProblemDTOById(id, true));
    }

    @PostMapping("/run")
    public ResponseEntity<CodeRunResponse> runCode(@RequestBody CodeRunRequest request) {
        return ResponseEntity.ok(evaluationService.runSingleCode(request));
    }

    @PostMapping("/submit")
    public ResponseEntity<CodingSubmission> submitCodeGeneral(@RequestBody Map<String, Object> payload) {
        User user = authService.getCurrentAuthenticatedUser();
        Long problemId = Long.parseLong(payload.get("problemId").toString());
        String langStr = payload.getOrDefault("language", "JAVA").toString();
        ProgrammingLanguage language = ProgrammingLanguage.valueOf(langStr.toUpperCase());
        String code = payload.getOrDefault("sourceCode", "").toString();

        return ResponseEntity.ok(evaluationService.submitCodingProblem(user, problemId, language, code));
    }

    @PostMapping("/problems/{id}/submit")
    public ResponseEntity<CodingSubmission> submitProblem(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload
    ) {
        User user = authService.getCurrentAuthenticatedUser();
        String langStr = payload.getOrDefault("language", "JAVA");
        ProgrammingLanguage language = ProgrammingLanguage.valueOf(langStr.toUpperCase());
        String code = payload.getOrDefault("sourceCode", "");

        return ResponseEntity.ok(evaluationService.submitCodingProblem(user, id, language, code));
    }

    @GetMapping("/submissions/{problemId}")
    public ResponseEntity<List<CodingSubmission>> getSubmissionsForProblem(@PathVariable Long problemId) {
        User user = authService.getCurrentAuthenticatedUser();
        return ResponseEntity.ok(submissionRepository.findByUserIdAndCodingProblemIdOrderBySubmittedAtDesc(user.getId(), problemId));
    }

    @GetMapping("/execution-status")
    public ResponseEntity<Map<String, Object>> getExecutionStatus() {
        Map<String, Object> status = new HashMap<>();
        boolean isConfigured = codeExecutionService.isExternalServiceConfigured();
        status.put("externalServiceConfigured", isConfigured);
        status.put("serviceName", isConfigured ? "Judge0 CE Service" : "Secure Fallback Sandbox");
        status.put("supportedLanguages", List.of("JAVA", "PYTHON", "C", "CPP"));
        return ResponseEntity.ok(status);
    }
}
