package com.examsaathi.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class DailyActiveUserResponse {
    private LocalDate date;
    private long count;
}
