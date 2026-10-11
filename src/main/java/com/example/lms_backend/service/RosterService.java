package com.example.lms_backend.service;

import com.example.lms_backend.dto.CourseRoster;
import com.example.lms_backend.dto.RosterItem;
import com.example.lms_backend.entity.Enrollment;
import com.example.lms_backend.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RosterService {

    private final EnrollmentRepository enrollmentRepository;

    @Transactional(readOnly = true)
    @Cacheable(
            value = "course_roster",
            key = "T(com.example.lms_backend.tenant.TenantContext).getTenantId() + ':' + #a0"
    )
    public CourseRoster getRoster(Long courseId) {
        log.info("==> [REDIS CACHE MISS] Fetching course {} roster from database", courseId);
        List<Enrollment> enrollments = enrollmentRepository.findAllByCourseId(courseId);

        List<RosterItem> items = enrollments.stream()
                .map(e -> RosterItem.builder()
                        .enrollmentId(e.getId())
                        .studentId(e.getStudent().getId())
                        .studentName(e.getStudent().getName())
                        .studentEmail(e.getStudent().getEmail())
                        .grade(e.getGrade())
                        .build())
                .toList();

        return new CourseRoster(items);
    }

    @CacheEvict(
            value = "course_roster",
            key = "T(com.example.lms_backend.tenant.TenantContext).getTenantId() + ':' + #a0"
    )
    public void evictRoster(Long courseId) {
        log.info("==> [REDIS CACHE EVICT] Evicted roster cache for course {}", courseId);
    }
}
