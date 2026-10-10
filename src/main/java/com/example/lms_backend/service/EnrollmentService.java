package com.example.lms_backend.service;

import com.example.lms_backend.entity.Course;
import com.example.lms_backend.entity.Enrollment;
import com.example.lms_backend.entity.Student;
import com.example.lms_backend.exception.DuplicateEnrollmentException;
import com.example.lms_backend.exception.InvalidGradeException;
import com.example.lms_backend.exception.ResourceNotFoundException;
import com.example.lms_backend.repository.CourseRepository;
import com.example.lms_backend.repository.EnrollmentRepository;
import com.example.lms_backend.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final StudentRepository studentRepository;

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

        return enrollmentRepository.save(enrollment);
    }

    public Enrollment setGrade(Long courseId, Long studentId, BigDecimal grade) {
        validateGradeRange(grade);

        Enrollment enrollment = enrollmentRepository.findByCourseIdAndStudentId(courseId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found for course id: " + courseId + " and student id: " + studentId));

        enrollment.setGrade(grade);
        return enrollmentRepository.save(enrollment);
    }

    public boolean dropStudent(Long courseId, Long studentId) {
        Enrollment enrollment = enrollmentRepository.findByCourseIdAndStudentId(courseId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found for course id: " + courseId + " and student id: " + studentId));

        enrollmentRepository.delete(enrollment);
        return true;
    }

    private void validateGradeRange(BigDecimal grade) {
        if (grade == null) {
            throw new InvalidGradeException("Grade cannot be null.");
        }
        if (grade.compareTo(BigDecimal.ZERO) < 0 || grade.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Grade must be between 0 and 100. Provided grade: " + grade);
        }
    }
}
