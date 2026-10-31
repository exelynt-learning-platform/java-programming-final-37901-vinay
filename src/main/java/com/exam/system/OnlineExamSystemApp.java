package com.exam.system;

import com.exam.system.config.DatabaseConnectionManager;
import com.exam.system.controller.ExamSystemController;
import com.exam.system.factory.QuestionFactory;
import com.exam.system.model.Difficulty;
import com.exam.system.model.Exam;
import com.exam.system.model.ExamAnalytics;
import com.exam.system.model.ExamResult;
import com.exam.system.model.ExamSubmission;
import com.exam.system.model.Question;
import com.exam.system.model.QuestionType;
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
import com.exam.system.strategy.EvaluationStrategy;
import com.exam.system.strategy.PenaltyEvaluationStrategy;
import com.exam.system.strategy.StandardEvaluationStrategy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * =============================================================================
 *          ONLINE EXAMINATION & RESULT PROCESSING SYSTEM (BACKEND)
 * =============================================================================
 * Complete backend orchestration demonstrating Core Java, Java 8 Features,
 * Multithreading, JDBC Persistence, Exception Handling, and GoF Design Patterns.
 */
public class OnlineExamSystemApp {

    public static void main(String[] args) {
        System.out.println("==========================================================================");
        System.out.println("   ONLINE EXAMINATION & RESULT PROCESSING SYSTEM (CAPSTONE BACKEND)       ");
        System.out.println("==========================================================================");

        // Reset database tables for reproducible clean run
        DatabaseConnectionManager.getInstance().resetDatabase();

        // ---------------------------------------------------------------------
        // 1. Dependency Injection & Service Wiring
        // ---------------------------------------------------------------------
        UserService userService = new UserServiceImpl();
        QuestionService questionService = new QuestionServiceImpl();
        ExamService examService = new ExamServiceImpl(questionService);
        EvaluationService evaluationService = new EvaluationServiceImpl(examService, questionService, userService);
        ReportService reportService = new ReportServiceImpl();

        ExamSystemController controller = new ExamSystemController(
                userService, questionService, examService, evaluationService, reportService
        );

        // =====================================================================
        // SECTION 1: USER MANAGEMENT & ROLE-BASED ACCESS
        // =====================================================================
        printBanner("SECTION 1: USER MANAGEMENT & AUTHENTICATION");

        // 1.1 Register Admin & Students
        User admin = controller.registerAdmin("admin_vinay", "AdminPass@2026", "Vinay Madarkhandi", "vinay@exam.edu");
        User alice = controller.registerStudent("student_alice", "PassAlice123", "Alice Johnson", "alice@exam.edu");
        User bob = controller.registerStudent("student_bob", "PassBob123", "Bob Smith", "bob@exam.edu");
        User charlie = controller.registerStudent("student_charlie", "PassCharlie123", "Charlie Brown", "charlie@exam.edu");
        User diana = controller.registerStudent("student_diana", "PassDiana123", "Diana Prince", "diana@exam.edu");

        System.out.println("Registered Users in Database:");
        System.out.println("  [Admin]   " + admin);
        System.out.println("  [Student] " + alice);
        System.out.println("  [Student] " + bob);
        System.out.println("  [Student] " + charlie);
        System.out.println("  [Student] " + diana);

        // 1.2 Login Validation
        System.out.println("\nTesting Login Validation:");
        User authenticatedUser = controller.login("admin_vinay", "AdminPass@2026");
        System.out.println("  Authentication Successful for: " + authenticatedUser.getFullName()
                + " (Role: " + authenticatedUser.getRole() + ")");

        // 1.3 Role-based access validation (Preventing unauthorized admin access)
        try {
            System.out.println("\nTesting Role-Based Access Control (Student attempting admin task):");
            controller.createExam(alice, "Hacked Exam", "Security", 10, "2026-11-01", 50.0);
        } catch (Exception ex) {
            System.out.println("  Security Intercepted Expected Exception: " + ex.getMessage());
        }

        // =====================================================================
        // SECTION 2: QUESTION BANK MANAGEMENT (ADMIN)
        // =====================================================================
        printBanner("SECTION 2: QUESTION BANK MANAGEMENT (FACTORY PATTERN)");

        // 2.1 Create polymorphic questions via QuestionFactory
        Question q1 = QuestionFactory.createQuestion(0,
                "Which interface does a class implement to enable object serialization?",
                "Java Core", Difficulty.EASY, QuestionType.MCQ,
                Arrays.asList("Serializable", "Cloneable", "Comparable", "Runnable"),
                "Serializable", 2.0);

        Question q2 = QuestionFactory.createQuestion(0,
                "Is java.lang.String class immutable in Java?",
                "Java Core", Difficulty.EASY, QuestionType.TRUE_FALSE,
                Arrays.asList("True", "False"),
                "True", 1.0);

        Question q3 = QuestionFactory.createQuestion(0,
                "What is the time complexity of HashSet.contains() in Big-O notation when n=1? (Enter 1)",
                "Java Core", Difficulty.MEDIUM, QuestionType.NUMERICAL,
                null, "1", 2.0);

        Question q4 = QuestionFactory.createQuestion(0,
                "Which keyword prevents a method from being overridden in a subclass?",
                "Java Core", Difficulty.MEDIUM, QuestionType.MCQ,
                Arrays.asList("static", "final", "abstract", "synchronized"),
                "final", 2.0);

        Question q5 = QuestionFactory.createQuestion(0,
                "Does a ConcurrentHashMap allow null keys?",
                "Java Core", Difficulty.HARD, QuestionType.TRUE_FALSE,
                Arrays.asList("True", "False"),
                "False", 3.0);

        // Add to Question Bank
        q1 = controller.addQuestion(admin, q1);
        q2 = controller.addQuestion(admin, q2);
        q3 = controller.addQuestion(admin, q3);
        q4 = controller.addQuestion(admin, q4);
        q5 = controller.addQuestion(admin, q5);

        System.out.printf("Added %d questions to database repository with automated caching.%n", 5);

        // 2.2 Question Filtering using Java 8 Streams & Lambdas
        System.out.println("\nFiltering Questions by Subject 'Java Core' and Difficulty 'MEDIUM':");
        List<Question> mediumQuestions = controller.getQuestionsByFilter("Java Core", Difficulty.MEDIUM);
        mediumQuestions.forEach(q -> System.out.println("  " + q));

        // =====================================================================
        // SECTION 3: EXAM MANAGEMENT
        // =====================================================================
        printBanner("SECTION 3: EXAM CREATION & SCHEDULING");

        Exam javaExam = controller.createExam(
                admin,
                "Java Enterprise Developer Certification 2026",
                "Java Core",
                60, // 60 minutes
                "2026-11-15 10:00:00",
                60.0 // 60% passing percentage
        );

        // Assign questions
        List<Integer> assignedIds = Arrays.asList(q1.getId(), q2.getId(), q3.getId(), q4.getId(), q5.getId());
        controller.assignQuestions(admin, javaExam.getId(), assignedIds);

        System.out.println("Exam Created Successfully:");
        System.out.println("  Title          : " + javaExam.getTitle());
        System.out.println("  Duration       : " + javaExam.getDurationMinutes() + " Minutes");
        System.out.println("  Schedule       : " + javaExam.getScheduledTime());
        System.out.println("  Passing Target : " + javaExam.getPassingPercentage() + "%");
        System.out.println("  Assigned Qs    : " + assignedIds.size() + " Questions");

        // =====================================================================
        // SECTION 4: EXAM ATTEMPTS (MULTITHREADING & TIMER AUTO-SUBMIT)
        // =====================================================================
        printBanner("SECTION 4: EXAM ATTEMPTS (MULTITHREADED TIMERS & AUTO-SUBMIT)");

        // Alice's Answers (High performer: 5/5 correct)
        Map<Integer, String> aliceAnswers = new HashMap<>();
        aliceAnswers.put(q1.getId(), "Serializable");
        aliceAnswers.put(q2.getId(), "True");
        aliceAnswers.put(q3.getId(), "1");
        aliceAnswers.put(q4.getId(), "final");
        aliceAnswers.put(q5.getId(), "False");

        // Bob's Answers (Good performer: 4/5 correct)
        Map<Integer, String> bobAnswers = new HashMap<>();
        bobAnswers.put(q1.getId(), "Serializable");
        bobAnswers.put(q2.getId(), "True");
        bobAnswers.put(q3.getId(), "1");
        bobAnswers.put(q4.getId(), "static"); // Incorrect
        bobAnswers.put(q5.getId(), "False");

        // Charlie's Answers (Time Expired! Auto-submitted with partial answers)
        Map<Integer, String> charlieAnswers = new HashMap<>();
        charlieAnswers.put(q1.getId(), "Serializable");
        charlieAnswers.put(q2.getId(), "False"); // Incorrect
        // Questions 3, 4, 5 were unattempted before time ran out!

        // Diana's Answers (Solid performer: 4/5 correct)
        Map<Integer, String> dianaAnswers = new HashMap<>();
        dianaAnswers.put(q1.getId(), "Serializable");
        dianaAnswers.put(q2.getId(), "True");
        dianaAnswers.put(q3.getId(), "1");
        dianaAnswers.put(q4.getId(), "final");
        dianaAnswers.put(q5.getId(), "True"); // Incorrect

        System.out.println("Starting simulated concurrent student exam sessions...");

        // Attempt 1: Alice on time
        ExamSubmission subAlice = controller.attemptExam(alice, javaExam.getId(), aliceAnswers, 5, false);
        System.out.println("  [Submission] Alice Johnson  : Status = SUBMITTED_ON_TIME");

        // Attempt 2: Bob on time
        ExamSubmission subBob = controller.attemptExam(bob, javaExam.getId(), bobAnswers, 5, false);
        System.out.println("  [Submission] Bob Smith      : Status = SUBMITTED_ON_TIME");

        // Attempt 3: Charlie - TIME EXPIRED! Multithreaded timer thread fires auto-submit
        ExamSubmission subCharlie = controller.attemptExam(charlie, javaExam.getId(), charlieAnswers, 1, true);
        System.out.println("  [Submission] Charlie Brown  : Status = AUTO_SUBMITTED (Time Expired)");

        // Attempt 4: Diana on time
        ExamSubmission subDiana = controller.attemptExam(diana, javaExam.getId(), dianaAnswers, 5, false);
        System.out.println("  [Submission] Diana Prince   : Status = SUBMITTED_ON_TIME");

        List<ExamSubmission> cohortSubmissions = Arrays.asList(subAlice, subBob, subCharlie, subDiana);

        // =====================================================================
        // SECTION 5: PARALLEL EVALUATION & TRANSACTIONAL BATCH PERSISTENCE
        // =====================================================================
        printBanner("SECTION 5: PARALLEL EVALUATION (EXECUTOR SERVICE & JDBC BATCH)");

        // Pluggable Strategy Pattern: Penalty Evaluation (Negative Marking)
        EvaluationStrategy scoringStrategy = new PenaltyEvaluationStrategy(0.25);
        System.out.println("Active Evaluation Strategy: " + scoringStrategy.getStrategyName());

        // Parallel evaluation across 3 thread pool workers
        int workerThreads = 3;
        List<ExamResult> evaluatedResults = controller.evaluateBatchParallel(
                cohortSubmissions, scoringStrategy, workerThreads
        );

        System.out.println("\nEvaluated & Persisted Results via JDBC Batch Transaction:");
        for (ExamResult res : evaluatedResults) {
            System.out.printf("  Candidate: %-18s | Score: %4.1f/%4.1f | Percentage: %5.1f%% | Grade: %-2s | Status: %s%n",
                    res.getStudentName(), res.getScore(), res.getMaxScore(),
                    res.getPercentage(), res.getGrade(), res.getStatus());
        }

        // =====================================================================
        // SECTION 6: ANALYTICS & REPORTING
        // =====================================================================
        printBanner("SECTION 6: EXAM ANALYTICS & LEADERBOARD REPORTS");

        // 6.1 Exam-wise Analytics using Java 8 Streams
        ExamAnalytics analytics = controller.getExamAnalytics(javaExam.getId());
        System.out.println("--- Exam Performance Analytics Report ---");
        System.out.println("  Exam Title               : " + analytics.getExamTitle());
        System.out.println("  Total Candidates Tested  : " + analytics.getTotalAttempts());
        System.out.println("  Candidates Passed        : " + analytics.getPassedCount());
        System.out.println("  Candidates Failed        : " + analytics.getFailedCount());
        System.out.printf("  Overall Pass Rate        : %.1f%%%n", analytics.getPassRatePercentage());
        System.out.printf("  Cohort Average Score     : %.2f marks%n", analytics.getAverageScore());
        System.out.printf("  Cohort Highest Score     : %.1f marks%n", analytics.getHighestScore());
        System.out.printf("  Cohort Lowest Score      : %.1f marks%n", analytics.getLowestScore());

        // 6.2 Top Performers Leaderboard
        System.out.println("\n--- Top Performers Leaderboard ---");
        List<ExamResult> leaderboard = controller.getTopPerformers(javaExam.getId(), 3);
        int rank = 1;
        for (ExamResult top : leaderboard) {
            System.out.printf("  Rank #%d: %-18s | Score: %4.1f/%4.1f (%.1f%%) | Grade: %s%n",
                    rank++, top.getStudentName(), top.getScore(), top.getMaxScore(),
                    top.getPercentage(), top.getGrade());
        }

        // 6.3 Student-wise Transcript for Alice
        System.out.println("\n--- Individual Student Transcript (Alice Johnson) ---");
        List<ExamResult> aliceHistory = controller.getStudentResults(alice.getId());
        for (ExamResult rec : aliceHistory) {
            System.out.printf("  Exam: %s | Score: %.1f/%.1f | Status: %s | Evaluated: %s%n",
                    rec.getExamTitle(), rec.getScore(), rec.getMaxScore(), rec.getStatus(), rec.getEvaluatedAt());
        }

        System.out.println("\n==========================================================================");
        System.out.println("Online Examination & Result Processing System demonstration complete!");
        System.out.println("==========================================================================");
    }

    private static void printBanner(String title) {
        System.out.println("\n--------------------------------------------------------------------------");
        System.out.println("  " + title);
        System.out.println("--------------------------------------------------------------------------");
    }
}
