package com.example.lms_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentEvent implements Serializable {
    private String eventId;
    private String eventType; // ENROLLED, GRADE_ASSIGNED, DROPPED
    private String tenantId;
    private Long courseId;
    private Long studentId;
    private BigDecimal grade;
    private Instant timestamp;
}
