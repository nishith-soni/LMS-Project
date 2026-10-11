package com.example.lms_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RosterItem implements Serializable {
    private Long enrollmentId;
    private Long studentId;
    private String studentName;
    private String studentEmail;
    private BigDecimal grade;
}
