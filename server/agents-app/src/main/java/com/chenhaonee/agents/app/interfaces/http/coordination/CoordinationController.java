package com.chenhaonee.agents.app.interfaces.http.coordination;

import com.chenhaonee.agents.app.application.coordination.CoordinationCommandApplicationService;
import com.chenhaonee.agents.app.application.coordination.CoordinationQueryApplicationService;
import com.chenhaonee.agents.app.interfaces.http.common.ExceptionHandlers;
import com.chenhaonee.agents.app.interfaces.http.common.Response;
import com.chenhaonee.agents.app.interfaces.http.coordination.dto.InstructionCreateRequest;
import com.chenhaonee.agents.app.interfaces.http.coordination.dto.InstructionDTO;
import com.chenhaonee.agents.app.interfaces.http.coordination.dto.QuestionsResolveRequest;
import com.chenhaonee.agents.app.interfaces.http.coordination.dto.QuestionsDTO;
import com.chenhaonee.agents.domain.coordination.model.Answer;
import com.chenhaonee.agents.domain.coordination.model.Instruction;
import com.chenhaonee.agents.domain.coordination.model.Questions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 协同交互接口，提供 Questions 和 Instruction 的用户交互能力。
 */
@Tag(name = "Coordination", description = "协同交互：问题集与指令")
@RestController
@RequestMapping("/api/tasks/{taskCode}/coordination")
@RequiredArgsConstructor
public class CoordinationController {

    private final CoordinationQueryApplicationService coordinationQueryApplicationService;
    private final CoordinationCommandApplicationService coordinationCommandApplicationService;

    @Operation(summary = "查询任务的问题集列表")
    @GetMapping("/questions")
    public Response<List<QuestionsDTO>> listQuestions(
            @Parameter(description = "任务编码") @PathVariable String taskCode,
            @Parameter(description = "过滤已解决/未解决的问题集")
            @RequestParam(required = false) Boolean resolved) {
        try {
            List<Questions> questions = coordinationQueryApplicationService.listQuestions(taskCode, resolved);
            return Response.success(questions.stream().map(this::toQuestionsDTO).toList());
        } catch (Exception e) {
            return Response.error(ExceptionHandlers.handleException(e));
        }
    }

    @Operation(summary = "查询问题集详情")
    @GetMapping("/questions/{questionsCode}")
    public Response<QuestionsDTO> getQuestions(
            @Parameter(description = "任务编码") @PathVariable String taskCode,
            @Parameter(description = "问题集编码") @PathVariable String questionsCode) {
        try {
            Questions questions = coordinationQueryApplicationService.getQuestionsByCode(questionsCode);
            return Response.success(toQuestionsDTO(questions));
        } catch (Exception e) {
            return Response.error(ExceptionHandlers.handleException(e));
        }
    }

    @Operation(summary = "回复问题集")
    @PostMapping("/questions/{questionsCode}/resolve")
    public Response<QuestionsDTO> resolveQuestions(
            @Parameter(description = "任务编码") @PathVariable String taskCode,
            @Parameter(description = "问题集编码") @PathVariable String questionsCode,
            @Valid @RequestBody QuestionsResolveRequest request) {
        try {
            List<Answer> answers = request.answers().stream()
                    .map(a -> new Answer(a.questionCode(), a.selectedOption(), a.userInput()))
                    .toList();
            Questions resolved = coordinationCommandApplicationService.resolveQuestions(taskCode, questionsCode, answers);
            return Response.success(toQuestionsDTO(resolved));
        } catch (Exception e) {
            return Response.error(ExceptionHandlers.handleException(e));
        }
    }

    @Operation(summary = "查询指令列表")
    @GetMapping("/instructions")
    public Response<List<InstructionDTO>> listInstructions(
            @Parameter(description = "任务编码") @PathVariable String taskCode) {
        try {
            List<Instruction> instructions = coordinationQueryApplicationService.listInstructions(taskCode);
            return Response.success(instructions.stream().map(this::toInstructionDTO).toList());
        } catch (Exception e) {
            return Response.error(ExceptionHandlers.handleException(e));
        }
    }

    @Operation(summary = "创建指令")
    @PostMapping("/instructions")
    public Response<InstructionDTO> createInstruction(
            @Parameter(description = "任务编码") @PathVariable String taskCode,
            @Valid @RequestBody InstructionCreateRequest request) {
        try {
            Instruction instruction = coordinationCommandApplicationService.createInstruction(
                    taskCode, request.turnCode(), request.content());
            return Response.success(toInstructionDTO(instruction));
        } catch (Exception e) {
            return Response.error(ExceptionHandlers.handleException(e));
        }
    }

    private QuestionsDTO toQuestionsDTO(Questions questions) {
        return new QuestionsDTO(
                questions.getCode(),
                questions.isResolved(),
                questions.getOpenedAt() != null ? questions.getOpenedAt().toString() : null,
                questions.getResolvedAt() != null ? questions.getResolvedAt().toString() : null,
                questions.getQuestions() != null
                        ? questions.getQuestions().stream().map(q -> new com.chenhaonee.agents.app.interfaces.http.coordination.dto.QuestionDTO(
                        q.code(), q.questionText(), q.options())).toList()
                        : List.of(),
                questions.getAnswers() != null
                        ? questions.getAnswers().stream().map(a -> new com.chenhaonee.agents.app.interfaces.http.coordination.dto.AnswerDTO(
                        a.questionCode(), a.selectedOption(), a.userInput())).toList()
                        : List.of());
    }

    private InstructionDTO toInstructionDTO(Instruction instruction) {
        return new InstructionDTO(
                instruction.getCode(),
                instruction.getTaskCode(),
                instruction.getTurnCode(),
                instruction.getContent(),
                instruction.getStatus().name(),
                instruction.getCreatedAt() != null ? instruction.getCreatedAt().toString() : null);
    }
}
