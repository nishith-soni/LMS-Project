package com.example.lms_backend.resolver;

import com.example.lms_backend.entity.Course;
import com.example.lms_backend.entity.Student;
import com.example.lms_backend.service.CourseService;
import com.example.lms_backend.service.StudentService;
import graphql.kickstart.tools.GraphQLQueryResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class QueryResolver implements GraphQLQueryResolver {

    private final CourseService courseService;
    private final StudentService studentService;

    public List<Course> getCourses() {
        return courseService.getAllCourses();
    }

    public Course getCourse(Long id) {
        return courseService.getCourseById(id);
    }

    public Student getStudent(Long id) {
        return studentService.getStudentById(id);
    }
}
