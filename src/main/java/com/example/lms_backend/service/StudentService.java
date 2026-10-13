package com.example.lms_backend.service;

import com.example.lms_backend.entity.Student;
import com.example.lms_backend.entity.Enrollment;
import com.example.lms_backend.repository.StudentRepository;
import com.example.lms_backend.repository.EnrollmentRepository;
import com.example.lms_backend.tenant.TenantContext;
import com.example.lms_backend.exception.ResourceNotFoundException;
import com.example.lms_backend.exception.InvalidInputException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentService {

    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;

    public Student getStudentById(Long id) {
        return studentRepository.findStudentById(id, TenantContext.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + id));
    }

    public List<Enrollment> getEnrollments(Long studentId) {
        return enrollmentRepository.findAllByStudentIdAndTenantId(studentId, TenantContext.getTenantId());
    }

    @Transactional
    public Student createStudent(String name, String email) {
        if (name == null || name.isBlank()) {
            throw new InvalidInputException("Student name cannot be blank.");
        }
        if (email == null || email.isBlank()) {
            throw new InvalidInputException("Student email cannot be blank.");
        }

        Student student = Student.builder()
                .name(name)
                .email(email)
                .build();
        return studentRepository.save(student);
    }
}
