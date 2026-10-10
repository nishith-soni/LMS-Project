Feature: Learning Management System Operations and Multi-Tenancy

  Scenario: Tenant isolation prevents cross-tenant data leaks
    Given I act as tenant "school-alpha"
    When I create a course with title "Data Structures"
    Then the courses list for tenant "school-alpha" contains "Data Structures"
    And the courses list for tenant "school-beta" does not contain "Data Structures"

  Scenario: Enroll a student in a course and assign a grade
    Given I act as tenant "school-alpha"
    When I create a course with title "Algorithms"
    And I create a student named "Bob" with email "bob@alpha.edu"
    And I enroll student "Bob" into course "Algorithms"
    And I assign grade 92.5 to student "Bob" in course "Algorithms"
    Then the enrollment for student "Bob" in course "Algorithms" has grade 92.5

  Scenario: Reject invalid grade outside the 0 to 100 range
    Given I act as tenant "school-alpha"
    When I create a course with title "Physics"
    And I create a student named "Charlie" with email "charlie@alpha.edu"
    And I enroll student "Charlie" into course "Physics"
    And I assign grade 105.0 to student "Charlie" in course "Physics"
    Then the response returns an error with code "INVALID_GRADE"