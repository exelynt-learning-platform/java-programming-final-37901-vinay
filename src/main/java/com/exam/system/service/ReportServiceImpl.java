package com.exam.system.service;

import com.exam.system.config.DatabaseConnectionManager;
import com.exam.system.model.ExamAnalytics;
import com.exam.system.model.ExamResult;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Implementation of ReportService utilizing Java 8 Streams for statistical analysis,
 * pass-rate aggregation, and top-performer rankings.
 */
public class ReportServiceImpl implements ReportService {

    private static final Logger logger = Logger.getLogger(ReportServiceImpl.class.getName());
    private final DatabaseConnectionManager dbManager = DatabaseConnectionManager.getInstance();

    @Override
    public List<ExamResult> getStudentResults(int studentId) {
        String sql = "SELECT r.id, r.exam_id, r.student_id, r.score, r.max_score, r.percentage, "
                   + "r.grade, r.status, r.evaluated_at, u.full_name, e.title "
                   + "FROM exam_results r "
                   + "JOIN users u ON r.student_id = u.id "
                   + "JOIN exams e ON r.exam_id = e.id "
                   + "WHERE r.student_id = ? ORDER BY r.id DESC";

        return executeQueryForResults(sql, studentId);
    }

    @Override
    public List<ExamResult> getExamResults(int examId) {
        String sql = "SELECT r.id, r.exam_id, r.student_id, r.score, r.max_score, r.percentage, "
                   + "r.grade, r.status, r.evaluated_at, u.full_name, e.title "
                   + "FROM exam_results r "
                   + "JOIN users u ON r.student_id = u.id "
                   + "JOIN exams e ON r.exam_id = e.id "
                   + "WHERE r.exam_id = ? ORDER BY r.score DESC";

        return executeQueryForResults(sql, examId);
    }

    /**
     * JAVA 8 STREAMS FEATURE:
     * Aggregates attempt totals, calculates mean score, determines cohort high/low,
     * and derives pass percentages using Stream collectors & summaryStatistics.
     */
    @Override
    public ExamAnalytics getExamAnalytics(int examId) {
        List<ExamResult> results = getExamResults(examId);
        String examTitle = results.isEmpty() ? ("Exam #" + examId) : results.get(0).getExamTitle();

        if (results.isEmpty()) {
            return new ExamAnalytics(examId, examTitle, 0, 0, 0, 0.0, 0.0, 0.0, 0.0);
        }

        int totalAttempts = results.size();

        // Stream filter to count passing students
        long passedCount = results.stream()
                .filter(r -> "PASSED".equalsIgnoreCase(r.getStatus()))
                .count();

        int failedCount = totalAttempts - (int) passedCount;
        double passRate = (passedCount * 100.0) / totalAttempts;

        // Stream mapToDouble & summaryStatistics
        DoubleSummaryStatistics scoreStats = results.stream()
                .mapToDouble(ExamResult::getScore)
                .summaryStatistics();

        return new ExamAnalytics(
                examId,
                examTitle,
                totalAttempts,
                (int) passedCount,
                failedCount,
                passRate,
                scoreStats.getAverage(),
                scoreStats.getMax(),
                scoreStats.getMin()
        );
    }

    /**
     * JAVA 8 STREAMS FEATURE:
     * Ranks candidates by score in descending order and extracts the top K performers.
     */
    @Override
    public List<ExamResult> getTopPerformers(int examId, int limit) {
        List<ExamResult> results = getExamResults(examId);

        return results.stream()
                .sorted(Comparator.comparingDouble(ExamResult::getScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    private List<ExamResult> executeQueryForResults(String sql, int param) {
        List<ExamResult> results = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ExamResult result = new ExamResult(
                            rs.getInt("id"),
                            rs.getInt("exam_id"),
                            rs.getInt("student_id"),
                            rs.getDouble("score"),
                            rs.getDouble("max_score"),
                            rs.getDouble("percentage"),
                            rs.getString("grade"),
                            rs.getString("status"),
                            rs.getString("evaluated_at")
                    );
                    result.setStudentName(rs.getString("full_name"));
                    result.setExamTitle(rs.getString("title"));
                    results.add(result);
                }
            }
        } catch (SQLException e) {
            logger.warning("Error querying results: " + e.getMessage());
        }
        return results;
    }
}
