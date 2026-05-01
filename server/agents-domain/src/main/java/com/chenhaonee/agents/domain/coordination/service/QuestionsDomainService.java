package com.chenhaonee.agents.domain.coordination.service;

import com.chenhaonee.agents.domain.coordination.model.Answer;
import com.chenhaonee.agents.domain.coordination.model.Question;
import com.chenhaonee.agents.domain.coordination.model.Questions;
import com.chenhaonee.agents.domain.coordination.repository.QuestionsRepository;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuestionsDomainService {

    private final QuestionsRepository questionsRepository;

    public QuestionsDomainService(QuestionsRepository questionsRepository) {
        this.questionsRepository = questionsRepository;
    }

    @Transactional(rollbackFor = Exception.class)
    public Questions createQuestions(String taskCode, String turnCode, List<Question> questions) {
        if (questions == null || questions.isEmpty()) {
            throw new IllegalArgumentException("questions cannot be empty");
        }
        Questions entity = new Questions();
        entity.initialize(taskCode, turnCode, questions);
        return questionsRepository.save(entity);
    }

    public Questions requireActive(String taskCode) {
        return questionsRepository.findFirstByTaskCodeAndResolvedFalseOrderByOpenedAtDesc(taskCode)
                .orElseThrow(() -> new NoSuchElementException("active questions not found for task: " + taskCode));
    }

    @Transactional(rollbackFor = Exception.class)
    public Questions resolveQuestions(String taskCode, String questionsCode, List<Answer> answers) {
        Questions questions = questionsRepository.findByCode(questionsCode)
                .orElseThrow(() -> new NoSuchElementException("questions not found: " + questionsCode));
        if (!questions.getTaskCode().equals(taskCode)) {
             throw new IllegalArgumentException("questions do not belong to task: " + taskCode);
        }
        questions.resolve(answers);
        return questionsRepository.save(questions);
    }

    public boolean hasPendingQuestions(String turnCode) {
        return questionsRepository.existsByTurnCodeAndResolved(turnCode, false);
    }

    public List<Answer> fetchAnswers(String questionsCode) {
        Questions questions = questionsRepository.findByCode(questionsCode)
                .orElseThrow(() -> new NoSuchElementException("questions not found: " + questionsCode));
        return questions.getAnswers() != null ? questions.getAnswers() : List.of();
    }
}
