package com.exam.system.service;

import com.exam.system.config.DatabaseConnectionManager;
import com.exam.system.exception.InvalidExamException;
import com.exam.system.exception.TimeExpiredException;
import com.exam.system.model.Exam;
import com.exam.system.model.ExamSubmission;
import com.exam.system.model.Question;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of ExamService integrating JDBC persistence and multithreaded timers.
 */
public class ExamServiceImpl implements ExamService {

    private static final Logger logger = Logger.getLogger(ExamServiceImpl.class.getName());
    private final DatabaseConnectionManager dbManager = DatabaseConnectionManager.getInstance();
    private final QuestionService questionService;

    public ExamServiceImpl(QuestionService questionService) {
        this.questionService = questionService;
    }

    @Override
    public Exam createExam(String title, String subject, int durationMinutes, String scheduledTime,
                           double passingPercentage) {
        if (title == null || title.trim().isEmpty()) {
            throw new InvalidExamException("Exam title cannot be empty.");
        }
        if (durationMinutes <= 0) {
            throw new InvalidExamException("Exam duration must be positive.");
        }

        String sql = "INSERT INTO exams (title, subject, duration_minutes, scheduled_time, total_marks, passing_percentage) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, title.trim());
            ps.setString(2, subject.trim());
            ps.setInt(3, durationMinutes);
            ps.setString(4, scheduledTime);
            ps.setDouble(5, 0.0); // Updated when questions are assigned
            ps.setDouble(6, passingPercentage);

            ps.executeUpdate();

            int id = 0;
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    id = rs.getInt(1);
                }
            }

            logger.info("Created Exam: " + title + " (ID: " + id + ")");
            return new Exam(id, title, subject, durationMinutes, scheduledTime, 0.0, passingPercentage);

        } catch (SQLException e) {
            throw new InvalidExamException("Failed to create exam: " + e.getMessage());
        }
    }

    @Override
    public void assignQuestionsToExam(int examId, List<Integer> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            throw new InvalidExamException("At least one question must be assigned to the exam.");
        }

        // Fetch questions to calculate total marks
        List<Question> questions = questionService.getQuestionsByIds(questionIds);
        double totalMarks = questions.stream().mapToDouble(Question::getPoints).sum();

        String insertQuestionSql = "INSERT OR IGNORE INTO exam_questions (exam_id, question_id) VALUES (?, ?)";
        String updateExamMarksSql = "UPDATE exams SET total_marks = ? WHERE id = ?";

        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false); // Transaction management
            try {
                try (PreparedStatement ps = conn.prepareStatement(insertQuestionSql)) {
                    for (int qId : questionIds) {
                        ps.setInt(1, examId);
                        ps.setInt(2, qId);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                try (PreparedStatement ps = conn.prepareStatement(updateExamMarksSql)) {
                    ps.setDouble(1, totalMarks);
                    ps.setInt(2, examId);
                    ps.executeUpdate();
                }

                conn.commit();
                logger.info(String.format("Assigned %d questions to Exam ID %d (Total Marks: %.1f)",
                        questionIds.size(), examId, totalMarks));

            } catch (SQLException e) {
                conn.rollback();
                throw new InvalidExamException("Transaction failed while assigning questions: " + e.getMessage());
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new InvalidExamException("Database error: " + e.getMessage());
        }
    }

    @Override
    public Optional<Exam> getExamById(int examId) {
        String examSql = "SELECT id, title, subject, duration_minutes, scheduled_time, total_marks, passing_percentage "
                       + "FROM exams WHERE id = ?";
        String questionIdsSql = "SELECT question_id FROM exam_questions WHERE exam_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement psExam = conn.prepareStatement(examSql);
             PreparedStatement psQuestions = conn.prepareStatement(questionIdsSql)) {

            psExam.setInt(1, examId);
            try (ResultSet rs = psExam.executeQuery()) {
                if (rs.next()) {
                    Exam exam = new Exam(
                            rs.getInt("id"),
                            rs.getString("title"),
                            rs.getString("subject"),
                            rs.getInt("duration_minutes"),
                            rs.getString("scheduled_time"),
                            rs.getDouble("total_marks"),
                            rs.getDouble("passing_percentage")
                    );

                    psQuestions.setInt(1, examId);
                    try (ResultSet rsQ = psQuestions.executeQuery()) {
                        while (rsQ.next()) {
                            exam.addQuestionId(rsQ.getInt("question_id"));
                        }
                    }
                    return Optional.of(exam);
                }
            }
        } catch (SQLException e) {
            logger.warning("Error fetching exam ID " + examId + ": " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<Exam> getAllExams() {
        List<Exam> list = new ArrayList<>();
        String sql = "SELECT id, title, subject, duration_minutes, scheduled_time, total_marks, passing_percentage FROM exams";

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Exam exam = new Exam(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("subject"),
                        rs.getInt("duration_minutes"),
                        rs.getString("scheduled_time"),
                        rs.getDouble("total_marks"),
                        rs.getDouble("passing_percentage")
                );
                list.add(exam);
            }
        } catch (SQLException e) {
            logger.warning("Error fetching exams: " + e.getMessage());
        }
        return list;
    }

    @Override
    public List<Question> getQuestionsForExam(int examId) {
        Optional<Exam> examOpt = getExamById(examId);
        if (examOpt.isEmpty()) {
            throw new InvalidExamException("Exam not found: " + examId);
        }
        return questionService.getQuestionsByIds(examOpt.get().getQuestionIds());
    }

    /**
     * MULTITHREADING FEATURE:
     * Simulates an active exam attempt with a dedicated background timer thread.
     * If forceExpire is true, the timer fires first and triggers automatic submission.
     */
    @Override
    public ExamSubmission startAndAttemptExam(int examId, int studentId,
                                              Map<Integer, String> answers,
                                              long simulateTimeSeconds,
                                              boolean forceExpire) {
        Exam exam = getExamById(examId)
                .orElseThrow(() -> new InvalidExamException("Cannot attempt non-existent exam: " + examId));

        AtomicBoolean isExpired = new AtomicBoolean(false);
        AtomicBoolean isSubmitted = new AtomicBoolean(false);

        // Dedicated Timer Thread
        Thread timerThread = new Thread(() -> {
            try {
                long durationMillis = forceExpire ? 200 : (simulateTimeSeconds * 1000L);
                Thread.sleep(durationMillis);

                if (!isSubmitted.get()) {
                    isExpired.set(true);
                    logger.warning(String.format("[TIMER THREAD] Exam ID %d for Student %d EXPIRED! Triggering auto-submit.",
                            examId, studentId));
                }
            } catch (InterruptedException ignored) {
                // Timer cancelled because student submitted on time
            }
        }, "ExamTimer-Exam" + examId + "-Student" + studentId);

        timerThread.start();

        // Simulate student taking exam
        try {
            if (forceExpire) {
                // Wait for timer to expire
                timerThread.join();
            } else {
                // Student completes before timer expires
                Thread.sleep(50);
                isSubmitted.set(true);
                timerThread.interrupt(); // Stop timer
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        boolean autoSubmitted = isExpired.get();
        if (autoSubmitted) {
            logger.info("Auto-submission executed due to time expiration for Student ID: " + studentId);
        } else {
            logger.info("Normal student submission received on time for Student ID: " + studentId);
        }

        return new ExamSubmission(examId, studentId, answers, autoSubmitted);
    }
}
