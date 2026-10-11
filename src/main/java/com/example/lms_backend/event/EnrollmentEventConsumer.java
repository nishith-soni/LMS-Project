package com.example.lms_backend.event;

import com.example.lms_backend.config.SqsConfig;
import com.example.lms_backend.dto.EnrollmentEvent;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EnrollmentEventConsumer {

    @SqsListener(SqsConfig.ENROLLMENT_QUEUE_NAME)
    public void handleEnrollmentEvent(EnrollmentEvent event) {
        log.info("<== [SQS CONSUMER] Received Event [{}]: Tenant='{}' | CourseId={} | StudentId={} | Grade={} | EventId={}",
                event.getEventType(),
                event.getTenantId(),
                event.getCourseId(),
                event.getStudentId(),
                event.getGrade(),
                event.getEventId()
        );
    }
}
