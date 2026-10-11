package com.example.lms_backend.resolver;

import com.example.lms_backend.dto.CourseRoster;
import com.example.lms_backend.dto.RosterItem;
import com.example.lms_backend.entity.Course;
import com.example.lms_backend.entity.Enrollment;
import com.example.lms_backend.entity.Student;
import com.example.lms_backend.service.RosterService;
import graphql.kickstart.tools.GraphQLResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CourseResolver implements GraphQLResolver<Course> {

    private final RosterService rosterService;

    public List<Enrollment> getEnrollments(Course course) {
        CourseRoster roster = rosterService.getRoster(course.getId());

        return roster.getItems().stream()
                .map(item -> {
                    Student student = Student.builder()
                            .name(item.getStudentName())
                            .email(item.getStudentEmail())
                            .build();
                    student.setId(item.getStudentId());

                    Enrollment enrollment = Enrollment.builder()
                            .grade(item.getGrade())
                            .course(course)
                            .student(student)
                            .build();
                    enrollment.setId(item.getEnrollmentId());
                    return enrollment;
                })
                .toList();
    }
}
