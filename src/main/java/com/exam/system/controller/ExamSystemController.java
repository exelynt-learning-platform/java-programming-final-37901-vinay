package com.exam.system.controller;

import com.exam.system.model.Difficulty;
import com.exam.system.model.Exam;
import com.exam.system.model.ExamAnalytics;
import com.exam.system.model.ExamResult;
import com.exam.system.model.ExamSubmission;
import com.exam.system.model.Question;
import com.exam.system.model.QuestionType;
import com.exam.system.model.Role;
import com.exam.system.model.User;
import com.exam.system.service.EvaluationService;
import com.exam.system.service.ExamService;
import com.exam.system.service.QuestionService;
import com.exam.system.service.ReportService;
import com.exam.system.service.UserService;
import com.exam.system.strategy.EvaluationStrategy;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * =============================================================================
 *                       BACKEND MVC CONTROLLER
 * =============================================================================
 * Central orchestrator connecting backend services, business models, and consumers.
 */
public class ExamSystemController {

    private final UserService userService;
    private final QuestionService questionService;
    private final ExamService examService;
    private final EvaluationService evaluationService;
    private final ReportService reportService;

    public ExamSystemController(UserService userService,
                                QuestionService questionService,
                                ExamService examService,
                                EvaluationService evaluationService,
                                ReportService reportService) {
        this.userService = userService;
        this.questionService = questionService;
        this.examService = examService;
        this.evaluationService = evaluationService;
        this.reportService = reportService;
    }

    // --- User Management ---

    public User registerAdmin(String username, String password, String fullName, String email) {
        return userService.registerUser(username, password, fullName, email, Role.ADMIN);
    }

    public User registerStudent(String username, String password, String fullName, String email) {
        return userService.registerUser(username, password, fullName, email, Role.STUDENT);
    }

    public User login(String username, String password) {
        return userService.authenticate(username, password);
    }

    // --- Question Management (Admin) ---

    public Question addQuestion(User admin, Question question) {
        userService.validateAdminAccess(admin);
        return questionService.addQuestion(question);
    }

    public Question updateQuestion(User admin, Question question) {
        userService.validateAdminAccess(admin);
        return questionService.updateQuestion(question);
    }

    public boolean deleteQuestion(User admin, int questionId) {
        userService.validateAdminAccess(admin);
        return questionService.deleteQuestion(questionId);
    }

    public List<Question> getQuestionsByFilter(String subject, Difficulty difficulty) {
        return questionService.getQuestionsBySubjectAndDifficulty(subject, difficulty);
    }

    // --- Exam Management ---

    public Exam createExam(User admin, String title, String subject, int durationMinutes,
                           String scheduledTime, double passingPercentage) {
        userService.validateAdminAccess(admin);
        return examService.createExam(title, subject, durationMinutes, scheduledTime, passingPercentage);
    }

    public void assignQuestions(User admin, int examId, List<Integer> questionIds) {
        userService.validateAdminAccess(admin);
        examService.assignQuestionsToExam(examId, questionIds);
    }

    public List<Question> getExamQuestions(int examId) {
        return examService.getQuestionsForExam(examId);
    }

    // --- Exam Attempt (Student) ---

    public ExamSubmission attemptExam(User student, int examId, Map<Integer, String> answers,
                                      long simulateSeconds, boolean forceExpire) {
        userService.validateStudentAccess(student);
        return examService.startAndAttemptExam(examId, student.getId(), answers, simulateSeconds, forceExpire);
    }

    // --- Evaluation & Batch Processing ---

    public ExamResult evaluateSubmission(ExamSubmission submission, EvaluationStrategy strategy) {
        ExamResult result = evaluationService.evaluateSingleSubmission(submission, strategy);
        evaluationService.saveResultsBatchWithTransaction(List.of(result));
        return result;
    }

    public List<ExamResult> evaluateBatchParallel(List<ExamSubmission> submissions,
                                                 EvaluationStrategy strategy,
                                                 int threadPoolSize) {
        List<ExamResult> results = evaluationService.evaluateBatchSubmissionsInParallel(
                submissions, strategy, threadPoolSize);
        evaluationService.saveResultsBatchWithTransaction(results);
        return results;
    }

    // --- Reports & Analytics ---

    public List<ExamResult> getStudentResults(int studentId) {
        return reportService.getStudentResults(studentId);
    }

    public ExamAnalytics getExamAnalytics(int examId) {
        return reportService.getExamAnalytics(examId);
    }

    public List<ExamResult> getTopPerformers(int examId, int limit) {
        return reportService.getTopPerformers(examId, limit);
    }
}
