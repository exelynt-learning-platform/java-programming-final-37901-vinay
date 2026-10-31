package com.exam.system;

import com.exam.system.config.DatabaseConnectionManager;
import com.exam.system.exception.AuthenticationException;
import com.exam.system.factory.QuestionFactory;
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
import com.exam.system.service.EvaluationServiceImpl;
import com.exam.system.service.ExamService;
import com.exam.system.service.ExamServiceImpl;
import com.exam.system.service.QuestionService;
import com.exam.system.service.QuestionServiceImpl;
import com.exam.system.service.ReportService;
import com.exam.system.service.ReportServiceImpl;
import com.exam.system.service.UserService;
import com.exam.system.service.UserServiceImpl;
import com.exam.system.strategy.PenaltyEvaluationStrategy;
import com.exam.system.strategy.StandardEvaluationStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JUnit 5 Unit Test Suite for Online Examination & Result Processing System.
 */
public class OnlineExamSystemTest {

    private UserService userService;
    private QuestionService questionService;
    private ExamService examService;
    private EvaluationService evaluationService;
    private ReportService reportService;

    @BeforeEach
    void setUp() {
        DatabaseConnectionManager.getInstance().resetDatabase();

        userService = new UserServiceImpl();
        questionService = new QuestionServiceImpl();
        examService = new ExamServiceImpl(questionService);
        evaluationService = new EvaluationServiceImpl(examService, questionService, userService);
        reportService = new ReportServiceImpl();
    }

    @Test
    @DisplayName("Verify Singleton DatabaseConnectionManager maintains single instance")
    void testDatabaseConnectionSingleton() {
        DatabaseConnectionManager instance1 = DatabaseConnectionManager.getInstance();
        DatabaseConnectionManager instance2 = DatabaseConnectionManager.getInstance();

        assertNotNull(instance1);
        assertEquals(instance1, instance2, "Both references must point to the identical Singleton instance");
    }

    @Test
    @DisplayName("Test User Registration and Authentication Success")
    void testUserRegistrationAndLogin() {
        User registered = userService.registerUser("test_student", "SecretPass1", "Test Student", "test@edu.com", Role.STUDENT);
        assertNotNull(registered);
        assertTrue(registered.getId() > 0);

        User authenticated = userService.authenticate("test_student", "SecretPass1");
        assertNotNull(authenticated);
        assertEquals("test_student", authenticated.getUsername());
        assertEquals(Role.STUDENT, authenticated.getRole());
    }

    @Test
    @DisplayName("Test Authentication with Invalid Credentials throws AuthenticationException")
    void testInvalidLoginThrowsException() {
        userService.registerUser("user1", "correct", "User One", "user1@edu.com", Role.STUDENT);

        assertThrows(AuthenticationException.class, () -> userService.authenticate("user1", "wrong_password"),
                "Wrong password must throw AuthenticationException");
        assertThrows(AuthenticationException.class, () -> userService.authenticate("non_existent", "pass"),
                "Unknown username must throw AuthenticationException");
    }

    @Test
    @DisplayName("Test QuestionFactory creates valid polymorphic questions")
    void testQuestionFactoryCreation() {
        Question mcq = QuestionFactory.createQuestion(1, "What is JVM?", "Java", Difficulty.EASY,
                QuestionType.MCQ, Arrays.asList("Virtual Machine", "Compiler"), "Virtual Machine", 2.0);
        assertTrue(mcq.evaluateAnswer("Virtual Machine"));
        assertFalse(mcq.evaluateAnswer("Compiler"));

        Question tf = QuestionFactory.createQuestion(2, "Java supports multiple inheritance for classes?", "Java",
                Difficulty.MEDIUM, QuestionType.TRUE_FALSE, Arrays.asList("True", "False"), "False", 1.0);
        assertTrue(tf.evaluateAnswer("False"));
        assertTrue(tf.evaluateAnswer("false"));
        assertFalse(tf.evaluateAnswer("True"));

        Question num = QuestionFactory.createQuestion(3, "Square root of 16?", "Math",
                Difficulty.EASY, QuestionType.NUMERICAL, null, "4.0", 1.0);
        assertTrue(num.evaluateAnswer("4"));
        assertTrue(num.evaluateAnswer("4.00"));
        assertFalse(num.evaluateAnswer("5"));
    }

    @Test
    @DisplayName("Test Standard Evaluation Strategy (No penalty)")
    void testStandardEvaluationStrategy() {
        Exam exam = new Exam(1, "Test Exam", "Java", 30, "2026-11-01", 10.0, 50.0);
        Question q1 = QuestionFactory.createQuestion(1, "Q1", "Java", Difficulty.EASY, QuestionType.TRUE_FALSE, null, "True", 5.0);
        Question q2 = QuestionFactory.createQuestion(2, "Q2", "Java", Difficulty.EASY, QuestionType.TRUE_FALSE, null, "False", 5.0);

        Map<Integer, String> studentAnswers = new HashMap<>();
        studentAnswers.put(1, "True");  // Correct: +5
        studentAnswers.put(2, "True");  // Incorrect: 0

        StandardEvaluationStrategy strategy = new StandardEvaluationStrategy();
        ExamResult result = strategy.evaluate(exam, 101, Arrays.asList(q1, q2), studentAnswers);

        assertEquals(5.0, result.getScore());
        assertEquals(10.0, result.getMaxScore());
        assertEquals(50.0, result.getPercentage());
        assertEquals("PASSED", result.getStatus());
    }

    @Test
    @DisplayName("Test Penalty Evaluation Strategy (Negative Marking)")
    void testPenaltyEvaluationStrategy() {
        Exam exam = new Exam(1, "Negative Marking Exam", "Java", 30, "2026-11-01", 10.0, 50.0);
        Question q1 = QuestionFactory.createQuestion(1, "Q1", "Java", Difficulty.EASY, QuestionType.TRUE_FALSE, null, "True", 4.0);
        Question q2 = QuestionFactory.createQuestion(2, "Q2", "Java", Difficulty.EASY, QuestionType.TRUE_FALSE, null, "False", 4.0);

        Map<Integer, String> studentAnswers = new HashMap<>();
        studentAnswers.put(1, "True");  // Correct: +4.0
        studentAnswers.put(2, "True");  // Incorrect: -1.0 (25% penalty on 4.0)

        PenaltyEvaluationStrategy strategy = new PenaltyEvaluationStrategy(0.25);
        ExamResult result = strategy.evaluate(exam, 101, Arrays.asList(q1, q2), studentAnswers);

        assertEquals(3.0, result.getScore()); // 4.0 - 1.0 = 3.0
        assertEquals(8.0, result.getMaxScore());
        assertEquals(37.5, result.getPercentage());
        assertEquals("FAILED", result.getStatus());
    }

    @Test
    @DisplayName("Test Multithreaded Exam Timer Auto-submission on Expiry")
    void testExamTimerAutoSubmit() {
        User student = userService.registerUser("timer_student", "pass", "Timer Student", "timer@edu.com", Role.STUDENT);
        Exam exam = examService.createExam("Timed Exam", "Java", 1, "2026-11-01", 40.0);

        Map<Integer, String> answers = new HashMap<>();
        answers.put(1, "A");

        // Force time expiry in timer thread
        ExamSubmission submission = examService.startAndAttemptExam(exam.getId(), student.getId(), answers, 1, true);

        assertNotNull(submission);
        assertTrue(submission.isAutoSubmitted(), "Exam submission must mark isAutoSubmitted = true when time expires");
    }
}
