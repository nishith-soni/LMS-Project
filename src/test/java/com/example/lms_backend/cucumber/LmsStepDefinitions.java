package com.example.lms_backend.cucumber;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class LmsStepDefinitions extends CucumberSpringConfiguration {

    private String currentTenant;
    private ResponseEntity<Map> lastResponse;
    private final Map<String, Long> courseIds = new HashMap<>();
    private final Map<String, Long> studentIds = new HashMap<>();

    @Given("I act as tenant {string}")
    public void iActAsTenant(String tenantId) {
        this.currentTenant = tenantId;
    }

    @When("I create a course with title {string}")
    public void iCreateACourseWithTitle(String title) {
        String mutation = String.format(
                "mutation { createCourse(title: \"%s\", description: \"Course %s\") { id title } }",
                title, title
        );
        lastResponse = executeGraphQL(mutation, currentTenant);
        Map data = (Map) lastResponse.getBody().get("data");
        Map course = (Map) data.get("createCourse");
        courseIds.put(title, Long.parseLong(course.get("id").toString()));
    }

    @When("I create a student named {string} with email {string}")
    public void iCreateAStudentNamedWithEmail(String name, String email) {
        String mutation = String.format(
                "mutation { createStudent(name: \"%s\", email: \"%s\") { id name } }",
                name, email
        );
        lastResponse = executeGraphQL(mutation, currentTenant);
        Map data = (Map) lastResponse.getBody().get("data");
        Map student = (Map) data.get("createStudent");
        studentIds.put(name, Long.parseLong(student.get("id").toString()));
    }

    @When("I enroll student {string} into course {string}")
    public void iEnrollStudentIntoCourse(String studentName, String courseTitle) {
        Long cId = courseIds.get(courseTitle);
        Long sId = studentIds.get(studentName);
        String mutation = String.format(
                "mutation { enrollStudent(courseId: %d, studentId: %d) { id } }",
                cId, sId
        );
        lastResponse = executeGraphQL(mutation, currentTenant);
    }

    @When("I assign grade {double} to student {string} in course {string}")
    public void iAssignGradeToStudentInCourse(Double grade, String studentName, String courseTitle) {
        Long cId = courseIds.get(courseTitle);
        Long sId = studentIds.get(studentName);
        String mutation = String.format(
                "mutation { setGrade(courseId: %d, studentId: %d, grade: %.1f) { id grade } }",
                cId, sId, grade
        );
        lastResponse = executeGraphQL(mutation, currentTenant);
    }

    @Then("the courses list for tenant {string} contains {string}")
    public void theCoursesListForTenantContains(String tenantId, String expectedTitle) {
        String query = "query { courses { id title } }";
        ResponseEntity<Map> response = executeGraphQL(query, tenantId);
        String responseBody = response.getBody().toString();
        assertThat(responseBody).contains(expectedTitle);
    }

    @Then("the courses list for tenant {string} does not contain {string}")
    public void theCoursesListForTenantDoesNotContain(String tenantId, String unexpectedTitle) {
        String query = "query { courses { id title } }";
        ResponseEntity<Map> response = executeGraphQL(query, tenantId);
        String responseBody = response.getBody().toString();
        assertThat(responseBody).doesNotContain(unexpectedTitle);
    }

    @Then("the enrollment for student {string} in course {string} has grade {double}")
    public void theEnrollmentForStudentInCourseHasGrade(String studentName, String courseTitle, Double expectedGrade) {
        Long cId = courseIds.get(courseTitle);
        String query = String.format("query { course(id: %d) { enrollments { grade student { name } } } }", cId);
        ResponseEntity<Map> response = executeGraphQL(query, currentTenant);
        assertThat(response.getBody().toString()).contains(expectedGrade.toString());
    }

    @Then("the response returns an error with code {string}")
    public void theResponseReturnsAnErrorWithCode(String expectedCode) {
        assertThat(lastResponse.getBody()).containsKey("errors");
        assertThat(lastResponse.getBody().toString()).contains(expectedCode);
    }

    private ResponseEntity<Map> executeGraphQL(String queryString, String tenantId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (tenantId != null) {
            headers.set("tenant-id", tenantId);
        }

        Map<String, String> body = Map.of("query", queryString);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        return restTemplate.postForEntity("/graphql", request, Map.class);
    }
}
