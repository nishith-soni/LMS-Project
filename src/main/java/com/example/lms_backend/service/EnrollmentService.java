package com.example.lms_backend.service;

import com.example.lms_backend.dto.EnrollmentEvent;
import com.example.lms_backend.entity.Course;
import com.example.lms_backend.entity.Enrollment;
import com.example.lms_backend.entity.Student;
import com.example.lms_backend.event.EnrollmentEventPublisher;
import com.example.lms_backend.exception.DuplicateEnrollmentException;
import com.example.lms_backend.exception.InvalidGradeException;
import com.example.lms_backend.exception.ResourceNotFoundException;
import com.example.lms_backend.repository.CourseRepository;
import com.example.lms_backend.repository.EnrollmentRepository;
import com.example.lms_backend.repository.StudentRepository;
import com.example.lms_backend.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final StudentRepository studentRepository;
    private final RosterService rosterService;
    private final EnrollmentEventPublisher eventPublisher;

    public Enrollment enrollStudent(Long courseId, Long studentId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        if (enrollmentRepository.existsByCourseIdAndStudentId(courseId, studentId)) {
            throw new DuplicateEnrollmentException("Student with id " + studentId + " is already enrolled in course with id " + courseId);
        }

        Enrollment enrollment = Enrollment.builder()
                .course(course)
                .student(student)
                .build();

        Enrollment saved = enrollmentRepository.save(enrollment);
        rosterService.evictRoster(courseId); // Evict the roster cache for the course

        // Asynchronously publish event to SQS
        eventPublisher.publishEvent(EnrollmentEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("STUDENT_ENROLLED")
                .tenantId(TenantContext.getTenantId())
                .courseId(courseId)
                .studentId(studentId)
                .timestamp(Instant.now())
                .build());

        return saved;
    }

    public Enrollment setGrade(Long courseId, Long studentId, BigDecimal grade) {
        validateGradeRange(grade);

        Enrollment enrollment = enrollmentRepository.findByCourseIdAndStudentId(courseId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found for course id: " + courseId + " and student id: " + studentId));

        enrollment.setGrade(grade);
        Enrollment saved = enrollmentRepository.save(enrollment);
        rosterService.evictRoster(courseId); // Evict the roster cache for the course

        // Asynchronously publish event to SQS
        eventPublisher.publishEvent(EnrollmentEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("GRADE_ASSIGNED")
                .tenantId(TenantContext.getTenantId())
                .courseId(courseId)
                .studentId(studentId)
                .grade(grade)
                .timestamp(Instant.now())
                .build());

        return saved;
    }

    public boolean dropStudent(Long courseId, Long studentId) {
        Enrollment enrollment = enrollmentRepository.findByCourseIdAndStudentId(courseId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found for course id: " + courseId + " and student id: " + studentId));

        enrollmentRepository.delete(enrollment);
        rosterService.evictRoster(courseId); // Evict the roster cache for the course

        eventPublisher.publishEvent(EnrollmentEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("STUDENT_DROPPED")
                .tenantId(TenantContext.getTenantId())
                .courseId(courseId)
                .studentId(studentId)
                .timestamp(Instant.now())
                .build());

        return true;
    }

    private void validateGradeRange(BigDecimal grade) {
        if (grade == null) {
            throw new InvalidGradeException("Grade cannot be null.");
        }
        if (grade.compareTo(BigDecimal.ZERO) < 0 || grade.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new InvalidGradeException("Grade must be between 0 and 100. Provided grade: " + grade);
        }
    }
}
