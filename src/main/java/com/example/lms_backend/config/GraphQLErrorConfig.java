package com.example.lms_backend.config;

import com.example.lms_backend.exception.DuplicateEnrollmentException;
import com.example.lms_backend.exception.InvalidGradeException;
import com.example.lms_backend.exception.ResourceNotFoundException;
import graphql.ExceptionWhileDataFetching;
import graphql.GraphQLError;
import graphql.kickstart.execution.error.GraphQLErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
public class GraphQLErrorConfig {

    @Bean
    public GraphQLErrorHandler errorHandler() {
        return new GraphQLErrorHandler() {
            @Override
            public List<GraphQLError> processErrors(List<GraphQLError> errors) {
                return errors.stream()
                        .map(this::transformError)
                        .collect(Collectors.toList());
            }

            private GraphQLError transformError(GraphQLError error) {
                if (error instanceof ExceptionWhileDataFetching fetchingError) {
                    Throwable exception = fetchingError.getException();
                    if (exception instanceof ResourceNotFoundException) {
                        return buildError(fetchingError, exception.getMessage(), "RESOURCE_NOT_FOUND");
                    }
                    if (exception instanceof DuplicateEnrollmentException) {
                        return buildError(fetchingError, exception.getMessage(), "DUPLICATE_ENROLLMENT");
                    }
                    if (exception instanceof InvalidGradeException) {
                        return buildError(fetchingError, exception.getMessage(), "INVALID_GRADE");
                    }
                }
                return error;
            }

            private GraphQLError buildError(ExceptionWhileDataFetching source, String message, String code) {
                return GraphQLError.newError()
                        .message(message)
                        .locations(source.getLocations())
                        .path(source.getPath())
                        .extensions(Map.of("code", code))
                        .build();
            }
        };
    }
}
