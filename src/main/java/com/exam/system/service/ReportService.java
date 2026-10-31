package com.exam.system.service;

import com.exam.system.model.ExamAnalytics;
import com.exam.system.model.ExamResult;

import java.util.List;

/**
 * Service contract for Results Reporting, Statistical Analytics, and Top Performer Rankings.
 */
public interface ReportService {

    List<ExamResult> getStudentResults(int studentId);

    List<ExamResult> getExamResults(int examId);

    ExamAnalytics getExamAnalytics(int examId);

    List<ExamResult> getTopPerformers(int examId, int limit);
}
