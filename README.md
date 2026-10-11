# LMS Backend — Multi-Tenant Service

## 1. Overview

This project is a multi-tenant Learning Management System (LMS) backend built with Spring Boot 3.2 and Java 17.

The service manages courses, students, enrollments, and grades while enforcing strict data isolation between schools (tenants) across both relational storage and in-memory caches:

* **Tenant Isolation**: Every database table includes a `tenant_id` column. Incoming requests supply a `tenant-id` HTTP header, which binds to a thread-local context and triggers a parameterized Hibernate session filter (`@Filter`). This appends tenant filtering to all queries automatically without polluting repository logic.
* **Tenant-Scoped Redis Caching (Stretch Goal)**: Course rosters are cached in Redis using compound tenant-aware keys (`course_roster::<tenantId>:<courseId>`) to prevent cross-tenant cache contamination. Roster data is serialized through a dedicated `CourseRoster` wrapper to avoid top-level collection deserialization issues. Caches automatically invalidate on enrollment creation, grade updates, and student drops.
* **Schema Versioning**: Database initialization and table definitions (`course`, `student`, `enrollment`) are managed strictly via Liquibase migrations.
* **API Layer**: Exposes a schema-first GraphQL API using Kickstart GraphQL (`graphql-spring-boot-starter`).
* **Automated Verification**: End-to-end multi-tenancy rules, roster caching, error handling, and business logic are tested via Cucumber BDD and JUnit 5.

### Tech Stack
* **Language & Runtime**: Java 17
* **Framework**: Spring Boot 3.2.5
* **API Engine**: Kickstart GraphQL 15.0.0
* **Persistence**: Spring Data JPA, Hibernate 6.4
* **Cache**: Redis 7.0 (Spring Cache + Spring Data Redis)
* **Database**: MySQL 8.0 (Dockerized)
* **Migrations**: Liquibase
* **BDD Testing**: Cucumber 7.15 + JUnit Platform

---

## 2. Docker & Infrastructure Setup

The infrastructure services (MySQL 8 and Redis 7) run containerized via `docker-compose.yml`.

### Start MySQL and Redis
```bash
docker compose up -d
```

### Check Container Status
```bash
docker ps
```
*Expected: Containers named `lms-mysql` (port 3306) and `lms-redis` (port 6379) with status `Up`.*

### Stop Containers
```bash
docker compose down
```

---

## 3. Running the Application

Ensure the Docker containers are running before launching the Spring Boot service. On startup, Liquibase will automatically detect the database and run any pending changesets.

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

## 4. Database & Cache Maintenance

### Clear the Redis Cache Only
To clear cached rosters without touching the database:
```bash
docker exec -it lms-redis redis-cli FLUSHALL
```

### Reset Both Database and Redis Cache (Clean Slate)
To wipe all data, reset MySQL tables, and clear Redis caches back to a clean initial state:
```bash
docker compose down -v && docker compose up -d
```
*(The `-v` flag removes the persistent Docker volumes. Spring Boot will execute Liquibase from scratch on next launch).*

---

## 5. Running Automated Tests

Run the full Cucumber BDD test suite:

* **Windows**:
  ```cmd
  mvnw.cmd test
  ```
* **macOS / Linux**:
  ```bash
  ./mvnw test
  ```

An interactive HTML report is generated upon test completion at:
```text
target/cucumber-reports.html
```

---

## 6. Sample GraphQL Operations

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

### 3. Enroll Student in Course (Triggers Cache Eviction)
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

### 4. Assign Grade to Student (Triggers Cache Eviction)
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

### 5. Fetch Course Roster (Redis Cached)
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
* *First Execution*: Misses cache, loads from MySQL, stores under key `course_roster::school-a:1` in Redis.
* *Subsequent Executions*: Served directly from Redis memory without database queries.

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

### 7. Drop a Student (Triggers Cache Eviction)
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
* **Expected Result**: GraphQL error with code `DUPLICATE_ENROLLMENT`.

#### Grade Out of Range Error
Run `setGrade` with a grade value outside `0.0`–`100.0` (e.g., `105.0`).
* **Expected Result**: GraphQL error with code `INVALID_GRADE`.