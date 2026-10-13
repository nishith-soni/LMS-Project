package com.example.lms_backend.repository;

import com.example.lms_backend.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    boolean existsByCourseIdAndStudentIdAndTenantId(Long courseId, Long studentId, String tenantId);
    Optional<Enrollment> findByCourseIdAndStudentIdAndTenantId(Long courseId, Long studentId, String tenantId);
    List<Enrollment> findAllByCourseIdAndTenantId(Long courseId, String tenantId);
    List<Enrollment> findAllByStudentIdAndTenantId(Long studentId, String tenantId);
}
