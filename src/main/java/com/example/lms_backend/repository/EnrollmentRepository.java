package com.example.lms_backend.repository;

import com.example.lms_backend.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    boolean existsByCourseIdAndStudentId(Long courseId, Long studentId);
    Optional<Enrollment> findByCourseIdAndStudentId(Long courseId, Long studentId);
}
