# LMS Backend — Multi-Tenant Service

## 1. Overview

This project is an enterprise-grade multi-tenant Learning Management System (LMS) backend built with Spring Boot 3.2 and Java 17.

The service manages courses, students, enrollments, and grading while maintaining strict data isolation between schools (tenants) across database queries, in-memory caching, and asynchronous event streams:

* **Dynamic Multi-Tenant Isolation**: Every database table includes a `tenant_id` column. Incoming requests supply a `tenant-id` HTTP header, which binds to a thread-safe `TenantContext` (`ThreadLocal`). A Spring AOP aspect (`TenantAspect`) intercepts service calls to activate Hibernate's parameterized `@Filter("tenantFilter")`. Single-entity lookups use explicit JPQL queries to guarantee that primary-key lookups enforce tenant boundaries and return `NOT_FOUND` across tenants.
* **Tenant-Scoped Redis Caching (Stretch Goal 1)**: Course rosters are cached in Redis using compound tenant-aware keys (`course_roster::<tenantId>:<courseId>`) to prevent cross-tenant cache contamination. Roster collections are encapsulated inside a dedicated `CourseRoster` DTO wrapper to avoid collection-level Jackson serialization pitfalls. Caches automatically invalidate on enrollment creation, grade updates, and student drops.
* **Asynchronous Event Pipeline via LocalStack SQS (Stretch Goal 2)**: Core business mutations decouple heavy downstream actions by publishing tenant-tagged events (`EnrollmentEvent`) to an Amazon SQS queue hosted locally via LocalStack. An asynchronous consumer (`@SqsListener`) processes these events in the background without blocking the GraphQL request thread.
* **Schema Migration & Versioning**: Database initialization and table definitions (`course`, `student`, `enrollment`) are strictly version-controlled using Liquibase YAML changesets.
* **API Layer**: Exposes a schema-first GraphQL API using Kickstart GraphQL (`graphql-spring-boot-starter`).
* **Automated BDD Verification**: End-to-end multi-tenancy rules, roster caching, validation boundaries, and GraphQL error mappings are tested via Cucumber BDD and JUnit 5.

### Tech Stack
* **Language & Runtime**: Java 17
* **Framework**: Spring Boot 3.2.5
* **API Engine**: Kickstart GraphQL 15.0.0
* **Persistence**: Spring Data JPA, Hibernate 6.4
* **Cache**: Redis 7.0 (Spring Cache + Spring Data Redis)
* **Messaging**: LocalStack 3.0 (AWS SQS) + Spring Cloud AWS SQS 3.1.1
* **Database**: MySQL 8.0 (Dockerized)
* **Migrations**: Liquibase
* **BDD Testing**: Cucumber 7.15 + JUnit Platform

---

## 2. Architecture & Design Patterns

```
                                 HTTP Request (Header: tenant-id)
                                                │
                                                ▼
                                    ┌───────────────────────┐
                                    │   TenantHttpFilter    │ (Extracts tenant-id into ThreadLocal)
                                    └───────────┬───────────┘
                                                │
                                                ▼
                                    ┌───────────────────────┐
                                    │    GraphQL Servlet    │
                                    └───────────┬───────────┘
                                                │
                                                ▼
                                    ┌───────────────────────┐
                                    │     TenantAspect      │ (Activates Hibernate tenantFilter)
                                    └───────────┬───────────┘
                                                │
                        ┌───────────────────────┴───────────────────────┐
                        ▼                                               ▼
             ┌─────────────────────┐                         ┌─────────────────────┐
             │   Course / Student  │                         │  EnrollmentService  │
             └──────────┬──────────┘                         └──────────┬──────────┘
                        │                                               │
                        │                                   ┌───────────┴───────────┐
                        ▼                                   ▼                       ▼
            ┌───────────────────────┐             ┌───────────────────┐   ┌───────────────────┐
            │     MySQL 8 (JPA)     │             │   RosterService   │   │  EventPublisher   │
            │ (where tenant_id = ?) │             │   (Redis Cache)   │   │  (LocalStack SQS) │
            └───────────────────────┘             └─────────┬─────────┘   └─────────┬─────────┘
                                                            │                       │
                                                            ▼                       ▼
                                                  course_roster::<tenant>   lms-enrollment-events
                                                                                    │
                                                                                    ▼
                                                                          ┌───────────────────┐
                                                                          │   EventConsumer   │
                                                                          │   (@SqsListener)  │
                                                                          └───────────────────┘
```

1. **Request Lifecycle**: `TenantHttpFilter` extracts the `tenant-id` header and sets it in `TenantContext`. `OpenEntityManagerInViewFilter` ensures the Hibernate session remains open across nested GraphQL field traversals.
2. **Data Layer Isolation**: Hibernate's `@Filter` appends `WHERE tenant_id = :tenantId` to queries. Entity inserts automatically populate `tenant_id` via a JPA `@PrePersist` lifecycle listener on `BaseTenantEntity`.
3. **Caching Strategy**: Course rosters are retrieved through `CourseResolver`. Cache keys combine tenant and course identifiers: `T(TenantContext).getTenantId() + ':' + #courseId`. When mutations alter enrollments, `RosterService.evictRoster()` purges the tenant-specific key.
4. **Event Pipeline**: Mutations publish `EnrollmentEvent` payloads carrying event type, tenant ID, course ID, student ID, and timestamps via `SqsTemplate`. An asynchronous `@SqsListener` on `lms-enrollment-events` consumes and processes messages decoupled from the API caller.

---

## 3. Docker & Infrastructure Setup

All supporting services (MySQL 8, Redis 7, and LocalStack) run containerized via `docker-compose.yml`.

### Start All Infrastructure Containers
```bash
docker compose up -d
```

### Check Container Health
```bash
docker ps
```
*Expected output: Three containers running with status `Up`:*
* `lms-mysql` on port `3306`
* `lms-redis` on port `6379`
* `lms-localstack` on port `4566`

### Stop Infrastructure Containers
```bash
docker compose down
```

---

## 4. Running the Application

Ensure Docker containers are running before launching the Spring Boot service. On startup:
* Liquibase detects MySQL and automatically applies pending changesets (`001-create-lms-tables.yaml`).
* `SqsConfig` checks LocalStack and ensures the queue `lms-enrollment-events` exists.

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

## 5. Maintenance & Reset Commands

### Clear Redis Cache Only
To clear cached rosters without dropping database records:
```bash
docker exec -it lms-redis redis-cli FLUSHALL
```

### Reset Database, Redis, and SQS Queues (Clean Slate)
To wipe all persistent data, drop MySQL tables, clear Redis caches, and reset LocalStack back to an empty initial state:
```bash
docker compose down -v && docker compose up -d
```
*(The `-v` flag removes the persistent Docker volumes. Spring Boot will re-execute Liquibase and queue initialization on next startup).*

---

## 6. Running Automated Tests

Execute the full BDD test suite:

* **Windows**:
  ```cmd
  mvnw.cmd test
  ```
* **macOS / Linux**:
  ```bash
  ./mvnw test
  ```

An interactive HTML test execution report is generated at:
```text
target/cucumber-reports.html
```

---

## 7. Sample GraphQL Operations

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
*Triggers: Database insert, Redis cache eviction, and SQS event publish (`STUDENT_ENROLLED`).*
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
* **Observed Server Logs**:
  ```text
  ==> [REDIS CACHE EVICT] Successfully evicted key 'school-a:1' from Redis
  ==> [SQS PRODUCER] Publishing event [STUDENT_ENROLLED] for Tenant: 'school-a', Course: 1, Student: 1
  <== [SQS CONSUMER] Received Event [STUDENT_ENROLLED]: Tenant='school-a' | CourseId=1 | StudentId=1 | Grade=null
  ```

---

### 4. Assign Grade to Student
*Triggers: Database update, Redis cache eviction, and SQS event publish (`GRADE_ASSIGNED`).*
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
* **Observed Server Logs**:
  ```text
  ==> [REDIS CACHE EVICT] Successfully evicted key 'school-a:1' from Redis
  ==> [SQS PRODUCER] Publishing event [GRADE_ASSIGNED] for Tenant: 'school-a', Course: 1, Student: 1
  <== [SQS CONSUMER] Received Event [GRADE_ASSIGNED]: Tenant='school-a' | CourseId=1 | StudentId=1 | Grade=95.5
  ```

---

### 5. Fetch Course Roster (Redis Caching in Action)
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
* *Execution 1*: Cache miss. Data loads from MySQL, stores under key `course_roster::school-a:1` in Redis.
  ```text
  ==> [REDIS CACHE MISS] Fetching course 1 roster from database
  ```
* *Execution 2*: Cache hit. Data returns directly from Redis without hitting MySQL.

---

### 6. Verify Multi-Tenant Data Isolation
Switch the tenant header to `school-b` and attempt to query the course:
* **Header**: `tenant-id: school-b`
* **Body**:
  ```graphql
  query {
    course(id: 1) {
      id
      title
    }
  }
  ```
* **Expected Response**:
  ```json
  {
    "errors": [
      {
        "message": "Course not found with id: 1",
        "extensions": {
          "code": "NOT_FOUND"
        }
      }
    ],
    "data": null
  }
  ```
  *(Returns `NOT_FOUND` because Course 1 belongs strictly to `school-a`).*

---

### 7. Drop a Student
*Triggers: Database deletion, Redis cache eviction, and SQS event publish (`STUDENT_DROPPED`).*
* **Header**: `tenant-id: school-a`
* **Body**:
  ```graphql
  mutation {
    dropStudent(courseId: 1, studentId: 1)
  }
  ```

---

### 8. Validation Error Scenarios

| Scenario | Trigger | Error Code |
|---|---|---|
| **Resource Not Found** | Querying a non-existent course ID or another tenant's resource | `NOT_FOUND` |
| **Duplicate Enrollment** | Enrolling the same student into the same course twice | `DUPLICATE_ENROLLMENT` |
| **Out-of-Bounds Grade** | Assigning a grade outside the `0.0`–`100.0` range (e.g., `105.0`) | `INVALID_GRADE` |