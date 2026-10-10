# LMS Backend — Multi-Tenant Service

## 1. Overview

This project is a multi-tenant Learning Management System (LMS) backend demo built with Spring Boot 3.2 and Java 17.

The application manages courses, students, and enrollments with grade assignments while enforcing strict data isolation between schools (tenants):
* **Tenant Isolation**: Every database table includes a `tenant_id` column. Incoming requests supply a `tenant-id` HTTP header, which binds to a thread-local context and triggers a parameterized Hibernate session filter (`@Filter`). This appends tenant filtering to all queries automatically without polluting repository logic.
* **Schema Versioning**: Database initialization and table definitions (`course`, `student`, `enrollment`) are managed strictly via Liquibase migrations.
* **API Layer**: Exposes a schema-first GraphQL API using Kickstart GraphQL (`graphql-spring-boot-starter`).
* **Automated Verification**: End-to-end multi-tenancy rules, error handling, and business logic are tested via Cucumber BDD and JUnit 5.

### Tech Stack
* **Language & Runtime**: Java 17
* **Framework**: Spring Boot 3.2.5
* **API Engine**: Kickstart GraphQL 15.0.0
* **Persistence**: Spring Data JPA, Hibernate 6.4
* **Database**: MySQL 8.0 (Dockerized)
* **Migrations**: Liquibase
* **BDD Testing**: Cucumber 7.15 + JUnit Platform

---

## 2. Docker & Database Setup

The database runs in a container defined in `docker-compose.yml`.

### Start the MySQL Container
```bash
docker compose up -d
```

### Check Container Status
```bash
docker ps
```
*Expected: A container named `lms-mysql` mapped to port `3306` with status `Up`.*

### Standalone Docker Run Alternative
If you prefer running the container without Docker Compose:
```bash
docker run -d \
  --name lms-mysql \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=rootpassword \
  -e MYSQL_DATABASE=lms_db \
  -e MYSQL_USER=lms_user \
  -e MYSQL_PASSWORD=lmspassword \
  mysql:8.0
```

### Stop the Database
```bash
docker compose down
```
*(To wipe persistent database volumes, use `docker compose down -v`)*

---

## 3. Running the Application

Ensure the Docker container is running before launching the Spring Boot service. On startup, Liquibase will automatically detect the database and run any pending changesets.

### Build the Project
* **Windows (CMD / PowerShell)**:
  ```cmd
  mvnw.cmd clean compile
  ```
* **macOS / Linux**:
  ```bash
  ./mvnw clean compile
  ```

### Start the Application
* **Windows (CMD / PowerShell)**:
  ```cmd
  mvnw.cmd spring-boot:run
  ```
* **macOS / Linux**:
  ```bash
  ./mvnw spring-boot:run
  ```

The GraphQL endpoint will listen for requests at:
```text
POST http://localhost:8080/graphql
```

---

## 4. Running Automated Tests

Run the full Cucumber BDD test suite:

* **Windows**:
  ```cmd
  mvnw.cmd test
  ```
* **macOS / Linux**:
  ```bash
  ./mvnw test
  ```

An HTML report will be generated upon completion at:
```text
target/cucumber-reports.html
```

---

## 5. Sample GraphQL Operations

Send requests to `http://localhost:8080/graphql` using Postman, Insomnia, or cURL. Every request requires the `tenant-id` header.

### 1. Create a Course
* **Header**: `tenant-id: school-a`
* **Body**:
  ```graphql
  mutation {
    createCourse(title: "CS101", description: "Introduction to Computer Science") {
      id
      title
      description
    }
  }
  ```

---

### 2. Create a Student
* **Header**: `tenant-id: school-a`
* **Body**:
  ```graphql
  mutation {
    createStudent(name: "Alice Smith", email: "alice@school-a.edu") {
      id
      name
      email
    }
  }
  ```

---

### 3. Enroll Student in Course
* **Header**: `tenant-id: school-a`
* **Body**:
  ```graphql
  mutation {
    enrollStudent(courseId: 1, studentId: 1) {
      id
      course {
        title
      }
      student {
        name
      }
    }
  }
  ```

---

### 4. Assign Grade to Student
* **Header**: `tenant-id: school-a`
* **Body**:
  ```graphql
  mutation {
    setGrade(courseId: 1, studentId: 1, grade: 95.5) {
      id
      grade
    }
  }
  ```

---

### 5. Fetch Course with Roster and Grades
* **Header**: `tenant-id: school-a`
* **Body**:
  ```graphql
  query {
    course(id: 1) {
      id
      title
      description
      enrollments {
        grade
        student {
          id
          name
          email
        }
      }
    }
  }
  ```

---

### 6. Verify Multi-Tenant Isolation
Switch the tenant header to `school-b` and query courses:
* **Header**: `tenant-id: school-b`
* **Body**:
  ```graphql
  query {
    courses {
      id
      title
    }
  }
  ```
* **Expected Response**:
  ```json
  {
    "data": {
      "courses": []
    }
  }
  ```
  *(Returns an empty list because the course belongs to `school-a`)*

---

### 7. Drop a Student
* **Header**: `tenant-id: school-a`
* **Body**:
  ```graphql
  mutation {
    dropStudent(courseId: 1, studentId: 1)
  }
  ```

---

### 8. Validation Error Cases

#### Duplicate Enrollment Error
Re-run `enrollStudent` with an existing course and student ID.
* **Expected Result**: Error code `DUPLICATE_ENROLLMENT`.

#### Grade Out of Range Error
Run `setGrade` with a grade value outside `0.0`–`100.0` (e.g., `105.0`).
* **Expected Result**: Error code `INVALID_GRADE`.