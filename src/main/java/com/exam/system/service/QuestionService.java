package com.exam.system.service;

import com.exam.system.model.Difficulty;
import com.exam.system.model.Question;

import java.util.List;
import java.util.Optional;

/**
 * Service contract for Question Bank Management and Caching.
 */
public interface QuestionService {

    Question addQuestion(Question question);

    Question updateQuestion(Question question);

    boolean deleteQuestion(int questionId);

    Optional<Question> getQuestionById(int questionId);

    List<Question> getAllQuestions();

    List<Question> getQuestionsBySubjectAndDifficulty(String subject, Difficulty difficulty);

    List<Question> getQuestionsByIds(List<Integer> questionIds);
}
