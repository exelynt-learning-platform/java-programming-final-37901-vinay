package com.exam.system.service;

import com.exam.system.config.DatabaseConnectionManager;
import com.exam.system.factory.QuestionFactory;
import com.exam.system.model.Difficulty;
import com.exam.system.model.Question;
import com.exam.system.model.QuestionType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Implementation of QuestionService incorporating in-memory question caching
 * and Java 8 Functional Streams for filtering and aggregation.
 */
public class QuestionServiceImpl implements QuestionService {

    private static final Logger logger = Logger.getLogger(QuestionServiceImpl.class.getName());
    private final DatabaseConnectionManager dbManager = DatabaseConnectionManager.getInstance();

    // Cache to optimize repeated lookups (Java Collections Map)
    private final Map<Integer, Question> questionCache = new ConcurrentHashMap<>();

    public QuestionServiceImpl() {
        loadCacheFromDatabase();
    }

    private void loadCacheFromDatabase() {
        String sql = "SELECT id, text, subject, difficulty, question_type, options, correct_answer, points FROM questions";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Question q = mapRowToQuestion(rs);
                questionCache.put(q.getId(), q);
            }
            logger.info("Question cache populated with " + questionCache.size() + " items.");
        } catch (SQLException e) {
            logger.warning("Error preloading question cache: " + e.getMessage());
        }
    }

    @Override
    public Question addQuestion(Question question) {
        String sql = "INSERT INTO questions (text, subject, difficulty, question_type, options, correct_answer, points) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, question.getText());
            ps.setString(2, question.getSubject());
            ps.setString(3, question.getDifficulty().name());
            ps.setString(4, question.getQuestionType().name());

            String optionsJoined = String.join("||", question.getOptions());
            ps.setString(5, optionsJoined);
            ps.setString(6, question.getCorrectAnswer());
            ps.setDouble(7, question.getPoints());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    question.setId(rs.getInt(1));
                }
            }

            questionCache.put(question.getId(), question);
            logger.info("Added question to database and cache (ID: " + question.getId() + ")");
            return question;

        } catch (SQLException e) {
            logger.severe("Database error adding question: " + e.getMessage());
            throw new RuntimeException("Failed to save question: " + e.getMessage(), e);
        }
    }

    @Override
    public Question updateQuestion(Question question) {
        String sql = "UPDATE questions SET text = ?, subject = ?, difficulty = ?, question_type = ?, "
                   + "options = ?, correct_answer = ?, points = ? WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, question.getText());
            ps.setString(2, question.getSubject());
            ps.setString(3, question.getDifficulty().name());
            ps.setString(4, question.getQuestionType().name());
            ps.setString(5, String.join("||", question.getOptions()));
            ps.setString(6, question.getCorrectAnswer());
            ps.setDouble(7, question.getPoints());
            ps.setInt(8, question.getId());

            int rows = ps.executeUpdate();
            if (rows > 0) {
                questionCache.put(question.getId(), question);
                logger.info("Updated question ID: " + question.getId());
            }
            return question;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update question: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteQuestion(int questionId) {
        String sql = "DELETE FROM questions WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, questionId);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                questionCache.remove(questionId);
                logger.info("Deleted question ID: " + questionId);
                return true;
            }
            return false;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete question: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Question> getQuestionById(int questionId) {
        if (questionCache.containsKey(questionId)) {
            return Optional.of(questionCache.get(questionId));
        }

        String sql = "SELECT id, text, subject, difficulty, question_type, options, correct_answer, points FROM questions WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, questionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Question q = mapRowToQuestion(rs);
                    questionCache.put(q.getId(), q);
                    return Optional.of(q);
                }
            }
        } catch (SQLException e) {
            logger.warning("Error fetching question " + questionId + ": " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<Question> getAllQuestions() {
        return new ArrayList<>(questionCache.values());
    }

    @Override
    public List<Question> getQuestionsBySubjectAndDifficulty(String subject, Difficulty difficulty) {
        // Java 8 Streams and Lambdas for declarative filtering
        return questionCache.values().stream()
                .filter(q -> subject == null || q.getSubject().equalsIgnoreCase(subject.trim()))
                .filter(q -> difficulty == null || q.getDifficulty() == difficulty)
                .collect(Collectors.toList());
    }

    @Override
    public List<Question> getQuestionsByIds(List<Integer> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return Collections.emptyList();
        }
        return questionIds.stream()
                .map(this::getQuestionById)
                .flatMap(Optional::stream)
                .collect(Collectors.toList());
    }

    private Question mapRowToQuestion(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String text = rs.getString("text");
        String subject = rs.getString("subject");
        Difficulty diff = Difficulty.valueOf(rs.getString("difficulty"));
        QuestionType type = QuestionType.valueOf(rs.getString("question_type"));
        String optionsStr = rs.getString("options");
        List<String> options = (optionsStr != null && !optionsStr.isEmpty())
                ? Arrays.asList(optionsStr.split("\\|\\|"))
                : Collections.emptyList();
        String answer = rs.getString("correct_answer");
        double points = rs.getDouble("points");

        return QuestionFactory.createQuestion(id, text, subject, diff, type, options, answer, points);
    }
}
