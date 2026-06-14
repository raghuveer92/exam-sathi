package com.examsaathi.dto.sheet;

import lombok.Builder;
import lombok.Data;

/**
 * Runtime question loaded from Google Sheets — not persisted as a DB entity.
 */
@Data
@Builder
public class SheetQuestion {
    private String id;
    private String exam;
    private String subject;
    private String topic;
    /** Optional stable DB ids from sheet columns exam_id, subject_id, topic_id */
    private String examId;
    private String subjectId;
    private String topicId;
    private String questionEn;
    private String questionHi;
    private String optionAEn;
    private String optionAHi;
    private String optionBEn;
    private String optionBHi;
    private String optionCEn;
    private String optionCHi;
    private String optionDEn;
    private String optionDHi;
    private String correctOption;
    private String difficulty;
    private String explanationEn;
    private String explanationHi;
    private String tags;
}
