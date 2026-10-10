package com.example.lms_backend.service;

import com.example.lms_backend.entity.Course;
import com.example.lms_backend.exception.ResourceNotFoundException;
import com.example.lms_backend.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;

    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    public Course getCourseById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));
    }

    @Transactional
    public Course createCourse(String title, String description) {
        Course course = Course.builder()
                .title(title)
                .description(description)
                .build();
        return courseRepository.save(course);
    }
}
