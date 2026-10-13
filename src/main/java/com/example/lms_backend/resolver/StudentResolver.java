package com.example.lms_backend.resolver;

import com.example.lms_backend.entity.Enrollment;
import com.example.lms_backend.entity.Student;
import com.example.lms_backend.service.StudentService;
import graphql.kickstart.tools.GraphQLResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class StudentResolver implements GraphQLResolver<Student> {

    private final StudentService studentService;

    public List<Enrollment> getEnrollments(Student student) {
        return studentService.getEnrollments(student.getId());
    }
}
