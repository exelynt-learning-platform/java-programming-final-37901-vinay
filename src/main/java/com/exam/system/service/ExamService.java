package com.exam.system.service;

import com.exam.system.model.Exam;
import com.exam.system.model.ExamSubmission;
import com.exam.system.model.Question;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service contract for Exam Management, Scheduling, and Exam Attempts with Multithreaded Timers.
 */
public interface ExamService {

    Exam createExam(String title, String subject, int durationMinutes, String scheduledTime,
                    double passingPercentage);

    void assignQuestionsToExam(int examId, List<Integer> questionIds);

    Optional<Exam> getExamById(int examId);

    List<Exam> getAllExams();

    List<Question> getQuestionsForExam(int examId);

    /**
     * Starts an exam session with a dedicated background timer thread.
     * Invokes auto-submit when the duration expires.
     */
    ExamSubmission startAndAttemptExam(int examId, int studentId,
                                      Map<Integer, String> answers,
                                      long simulateTimeSeconds,
                                      boolean forceExpire);
}
