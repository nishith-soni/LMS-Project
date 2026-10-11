package com.example.lms_backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;

@Slf4j
@Configuration
public class SqsConfig {

    public static final String ENROLLMENT_QUEUE_NAME = "lms-enrollment-events";

    @Bean
    public InitializingBean createQueueIfNotExists(SqsAsyncClient sqsAsyncClient) {
        return () -> {
            try {
                log.info("Ensuring SQS queue '{}' exists on LocalStack...", ENROLLMENT_QUEUE_NAME);
                sqsAsyncClient.createQueue(CreateQueueRequest.builder()
                                .queueName(ENROLLMENT_QUEUE_NAME)
                                .build())
                        .join();
                log.info("SQS queue '{}' is ready.", ENROLLMENT_QUEUE_NAME);
            } catch (Exception e) {
                log.warn("Queue creation note: {}", e.getMessage());
            }
        };
    }
}
