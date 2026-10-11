package com.example.lms_backend.event;

import com.example.lms_backend.config.SqsConfig;
import com.example.lms_backend.dto.EnrollmentEvent;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EnrollmentEventPublisher {

    private final SqsTemplate sqsTemplate;

    public void publishEvent(EnrollmentEvent event) {
        log.info("==> [SQS PRODUCER] Publishing event [{}] for Tenant: '{}', Course: {}, Student: {}",
                event.getEventType(), event.getTenantId(), event.getCourseId(), event.getStudentId());

        sqsTemplate.send(to -> to
                .queue(SqsConfig.ENROLLMENT_QUEUE_NAME)
                .payload(event)
        );
    }
}
