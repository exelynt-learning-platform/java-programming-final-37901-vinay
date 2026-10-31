package com.exam.system.service;

import com.exam.system.model.ExamResult;
import com.exam.system.model.ExamSubmission;
import com.exam.system.strategy.EvaluationStrategy;

import java.util.List;

/**
 * Service contract for Answer Evaluation, Parallel Batch Processing, and Result Persistence.
 */
public interface EvaluationService {

    ExamResult evaluateSingleSubmission(ExamSubmission submission, EvaluationStrategy strategy);

    List<ExamResult> evaluateBatchSubmissionsInParallel(List<ExamSubmission> submissions,
                                                        EvaluationStrategy strategy,
                                                        int threadPoolSize);

    void saveResultsBatchWithTransaction(List<ExamResult> results);
}
