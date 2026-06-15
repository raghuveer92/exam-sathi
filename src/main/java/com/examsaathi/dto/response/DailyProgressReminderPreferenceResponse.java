package com.examsaathi.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DailyProgressReminderPreferenceResponse {
    private Boolean enabled;
    private String reminderTime;
}
