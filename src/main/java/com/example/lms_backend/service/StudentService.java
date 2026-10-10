package com.example.lms_backend.service;

import com.example.lms_backend.entity.Student;
import com.example.lms_backend.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentService {

    private final StudentRepository studentRepository;

    public Student getStudentById(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + id));
    }

    @Transactional
    public Student createStudent(String name, String email) {
        Student student = Student.builder()
                .name(name)
                .email(email)
                .build();
        return studentRepository.save(student);
    }
}
