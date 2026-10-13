package com.example.lms_backend.repository;

import com.example.lms_backend.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    @Query("SELECT c FROM Course c WHERE c.id = :id AND c.tenantId = :tenantId")
    Optional<Course> findCourseById(@Param("id") Long id, @Param("tenantId") String tenantId);

    List<Course> findAllByTenantId(String tenantId);
}
