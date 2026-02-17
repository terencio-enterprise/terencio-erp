package es.terencio.erp.marketing.application.dto;

import lombok.Data;

@Data
public class UnsubscribeRequest {
    private String token;
    private String action; // SNOOZE, UNSUBSCRIBE, CHANGE_FREQ
    private Integer snoozeDays;
    private String reason;
}
