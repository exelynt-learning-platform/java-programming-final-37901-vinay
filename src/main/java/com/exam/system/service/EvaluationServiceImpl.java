package com.exam.system.service;

import com.exam.system.config.DatabaseConnectionManager;
import com.exam.system.exception.InvalidExamException;
import com.exam.system.model.Exam;
import com.exam.system.model.ExamResult;
import com.exam.system.model.ExamSubmission;
import com.exam.system.model.Question;
import com.exam.system.strategy.EvaluationStrategy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of EvaluationService:
 * 1. Evaluates submissions using pluggable EvaluationStrategy.
 * 2. Parallel evaluation using ExecutorService fixed thread pool.
 * 3. Batch JDBC inserts with full transactional ACID support (commit/rollback).
 */
public class EvaluationServiceImpl implements EvaluationService {

    private static final Logger logger = Logger.getLogger(EvaluationServiceImpl.class.getName());
    private final DatabaseConnectionManager dbManager = DatabaseConnectionManager.getInstance();
    private final ExamService examService;
    private final QuestionService questionService;
    private final UserService userService;

    public EvaluationServiceImpl(ExamService examService, QuestionService questionService, UserService userService) {
        this.examService = examService;
        this.questionService = questionService;
        this.userService = userService;
    }

    @Override
    public ExamResult evaluateSingleSubmission(ExamSubmission submission, EvaluationStrategy strategy) {
        Exam exam = examService.getExamById(submission.getExamId())
                .orElseThrow(() -> new InvalidExamException("Exam not found for ID: " + submission.getExamId()));

        List<Question> questions = questionService.getQuestionsByIds(exam.getQuestionIds());
        ExamResult result = strategy.evaluate(exam, submission.getStudentId(), questions, submission.getStudentAnswers());

        // Decorate metadata
        userService.getUserById(submission.getStudentId())
                .ifPresent(u -> result.setStudentName(u.getFullName()));
        result.setExamTitle(exam.getTitle());

        return result;
    }

    /**
     * MULTITHREADING FEATURE:
     * Evaluates multiple exam submissions concurrently across worker threads using ExecutorService.
     */
    @Override
    public List<ExamResult> evaluateBatchSubmissionsInParallel(List<ExamSubmission> submissions,
                                                               EvaluationStrategy strategy,
                                                               int threadPoolSize) {
        logger.info(String.format("Starting parallel evaluation of %d submissions across %d worker threads...",
                submissions.size(), threadPoolSize));

        ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);
        List<Future<ExamResult>> futures = new ArrayList<>();

        for (ExamSubmission sub : submissions) {
            Callable<ExamResult> task = () -> {
                String threadName = Thread.currentThread().getName();
                logger.fine(String.format("[%s] Evaluating submission from Student ID %d for Exam ID %d",
                        threadName, sub.getStudentId(), sub.getExamId()));
                return evaluateSingleSubmission(sub, strategy);
            };
            futures.add(executor.submit(task));
        }

        List<ExamResult> results = new ArrayList<>();
        for (Future<ExamResult> future : futures) {
            try {
                results.add(future.get());
            } catch (InterruptedException | ExecutionException e) {
                logger.log(Level.SEVERE, "Error in parallel evaluation worker: " + e.getMessage(), e);
            }
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        logger.info("Parallel evaluation completed for " + results.size() + " submissions.");
        return results;
    }

    /**
     * JDBC & DATABASE FEATURE:
     * Batch inserts exam evaluation results inside a single transactional boundary.
     * Guarantees all records commit together or roll back on error.
     */
    @Override
    public void saveResultsBatchWithTransaction(List<ExamResult> results) {
        if (results == null || results.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO exam_results (exam_id, student_id, score, max_score, percentage, grade, status, evaluated_at) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false); // Begin Transaction
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                for (ExamResult r : results) {
                    ps.setInt(1, r.getExamId());
                    ps.setInt(2, r.getStudentId());
                    ps.setDouble(3, r.getScore());
                    ps.setDouble(4, r.getMaxScore());
                    ps.setDouble(5, r.getPercentage());
                    ps.setString(6, r.getGrade());
                    ps.setString(7, r.getStatus());
                    ps.setString(8, r.getEvaluatedAt());
                    ps.addBatch(); // Batch insert optimization
                }

                int[] updateCounts = ps.executeBatch(); // Execute all inserts together
                conn.commit(); // Commit Transaction

                logger.info(String.format("Successfully committed batch of %d exam results to database.",
                        updateCounts.length));

            } catch (SQLException e) {
                conn.rollback(); // Rollback Transaction on Error
                logger.log(Level.SEVERE, "Transaction failed! Rolled back batch insert: " + e.getMessage(), e);
                throw new RuntimeException("Database transaction failed: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Connection error during result batch persistence: " + e.getMessage(), e);
        }
    }
}
