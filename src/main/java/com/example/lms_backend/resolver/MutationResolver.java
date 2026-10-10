package com.example.lms_backend.resolver;

import com.example.lms_backend.entity.Course;
import com.example.lms_backend.entity.Enrollment;
import com.example.lms_backend.entity.Student;
import com.example.lms_backend.service.CourseService;
import com.example.lms_backend.service.EnrollmentService;
import com.example.lms_backend.service.StudentService;
import graphql.kickstart.tools.GraphQLMutationResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class MutationResolver implements GraphQLMutationResolver {

    private final CourseService courseService;
    private final StudentService studentService;
    private final EnrollmentService enrollmentService;

    public Course createCourse(String title, String description) {
        return courseService.createCourse(title, description);
    }

    public Student createStudent(String name, String email) {
        return studentService.createStudent(name, email);
    }

    public Enrollment enrollStudent(Long courseId, Long studentId) {
        return enrollmentService.enrollStudent(courseId, studentId);
    }

    public Enrollment setGrade(Long courseId, Long studentId, Double grade) {
        return enrollmentService.setGrade(courseId, studentId, BigDecimal.valueOf(grade));
    }

    public Boolean dropStudent(Long courseId, Long studentId) {
        return enrollmentService.dropStudent(courseId, studentId);
    }
}
