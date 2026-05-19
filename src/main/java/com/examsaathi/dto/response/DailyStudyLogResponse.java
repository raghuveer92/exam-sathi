package com.examsaathi.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class DailyStudyLogResponse {
    private Long id;
    private LocalDate studyDate;
    private Double hoursStudied;
    private Integer topicsCompleted;
}
