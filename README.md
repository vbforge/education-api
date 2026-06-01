
<div align="center">

# 📚 EduFlow - Education Management System

![Java Version](https://img.shields.io/badge/Java-21-blue.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.0-brightgreen.svg)
![MySQL](https://img.shields.io/badge/MySQL-8.0-orange.svg)
![Docker](https://img.shields.io/badge/Docker-Ready-blue.svg)
![License](https://img.shields.io/badge/License-MIT-green.svg)

**A full-featured Learning Management System (LMS) built with Spring Boot**

</div>

---

## 📖 Overview

EduFlow is a comprehensive educational platform that enables seamless management of courses, students, instructors, and assignments. Built with modern Java technologies and featuring an Apple-inspired dark mode UI.

### 🎯 Key Functionality

- **Three User Roles**: Admin, Instructor, Student with role-based access control
- **Course Management**: Create, organize, and manage courses with modules and assignments
- **Assignment System**: Submit work, grade submissions, provide feedback
- **Progress Tracking**: Real-time progress calculation and grade analytics
- **Email Notifications**: Automated emails for enrollment, grading, and reminders
- **File Upload/Download**: Secure file handling for assignment submissions

---

## ✨ Features

### 👨‍🏫 **For Instructors**
- Create and manage courses, modules, and assignments
- Grade student submissions with detailed feedback
- View student progress and course analytics
- Post announcements to enrolled students
- Download student submissions

### 👨‍🎓 **For Students**
- Browse and enroll in courses
- Submit assignments with file uploads
- View grades and instructor feedback
- Track course progress
- Update profile and change password
- View submission history

### 👑 **For Admins**
- Complete user management (CRUD operations)
- System overview dashboard with analytics
- Course monitoring
- User role management

### 🔧 **Technical Features**
- Secure authentication with BCrypt password encoding
- RESTful API with Spring Boot
- File storage with validation and security
- Email notifications via SMTP (MailHog for development)
- Docker support for easy deployment
- Swagger/OpenAPI documentation

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|------------|
| **Backend** | Java 21, Spring Boot 3.5.0 |
| **Security** | Spring Security, BCrypt |
| **Database** | MySQL 8.0, H2 (testing) |
| **ORM** | Spring Data JPA, Hibernate |
| **Frontend** | Thymeleaf, HTML5, CSS3 |
| **Email** | JavaMailSender, MailHog |
| **Container** | Docker, Docker Compose |
| **API Docs** | Swagger/OpenAPI 3.0 |
| **Monitoring** | Spring Boot Actuator |

---

## 🚀 Quick Start

### Prerequisites

- Java 21+
- Maven 3.9+
- MySQL 8.0+ (or Docker)
- Docker & Docker Compose (optional)

### Local Development

```bash
# 1. Clone the repository
git clone https://github.com/yourusername/education-api.git
cd education-api

# 2. Configure database (application-local.yml)
# Update username and password for your MySQL

# 3. Build the project
./mvnw clean install

# 4. Run the application
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### Docker Setup (Recommended)

```bash
# Start all services (app + MySQL + MailHog)
docker-compose up -d --build

# View logs
docker-compose logs -f app

# Stop services
docker-compose down
```

### Default Credentials

| Role | Email | Password |
|------|-------|----------|
| 👑 Admin | admin@email.com | admin1234 |
| 👨‍🏫 Instructor | instructor@email.com | instructor1234 |
| 👨‍🎓 Student | student@email.com | student1234 |

> **Note**: You can also register new students via the registration page.

---

## 🌐 Access Points

| Service | URL | Description |
|---------|-----|-------------|
| Application | http://localhost:8080 | Main application |
| MailHog UI | http://localhost:8025 | Email testing interface |
| Actuator | http://localhost:8080/actuator | Health and metrics |
| Swagger UI | http://localhost:8080/swagger-ui.html | API documentation |

---

## 📁 Project Structure

```
education-api/
├── src/main/java/com/vbforge/educationapi/
│   ├── config/          # Security, Mail, Async configuration
│   ├── controller/      # REST and MVC controllers
│   ├── domain/          # JPA entities
│   ├── dto/             # Data Transfer Objects
│   ├── mapper/          # Entity ↔ DTO mappers
│   ├── repository/      # Spring Data JPA repositories
│   ├── service/         # Business logic layer
│   └── exception/       # Custom exceptions and handlers
├── src/main/resources/
│   ├── templates/       # Thymeleaf HTML templates
│   ├── application.yml  # Main configuration
│   └── application-*.yml # Profile-specific configs
├── docker-compose.yml   # Docker services
├── Dockerfile           # Multi-stage build
└── pom.xml             # Maven dependencies
```

---

## 📡 API Endpoints

<details>
<summary>Click to expand API endpoints</summary>

### Courses
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/courses` | Get all courses (paginated) |
| GET | `/api/v1/courses/{id}` | Get course by ID |
| POST | `/api/v1/courses` | Create course (INSTRUCTOR/ADMIN) |
| PUT | `/api/v1/courses/{id}` | Update course (INSTRUCTOR/ADMIN) |
| DELETE | `/api/v1/courses/{id}` | Delete course (ADMIN) |

### Modules
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/courses/{courseId}/modules` | Get modules by course |
| POST | `/api/v1/modules` | Create module |
| PUT | `/api/v1/modules/{id}` | Update module |
| DELETE | `/api/v1/modules/{id}` | Delete module |

### Assignments
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/modules/{moduleId}/assignments` | Get assignments by module |
| POST | `/api/v1/assignments` | Create assignment |
| PUT | `/api/v1/assignments/{id}` | Update assignment |
| DELETE | `/api/v1/assignments/{id}` | Delete assignment |

### Submissions
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/submissions` | Submit assignment (file upload) |
| PATCH | `/api/v1/submissions/{id}/grade` | Grade submission (INSTRUCTOR) |
| GET | `/api/v1/submissions/student/{studentId}` | Get student submissions |

</details>

---

## 🧪 Testing

```bash
# Run unit and integration tests
./mvnw test

# Run with specific profile
./mvnw test -Dspring.profiles.active=test
```

---

## 📊 Database Schema

```
courses ────── modules ────── assignments
  │              │                │
  │              │                └── submissions
  │              │                      │
  │              └──────────────────────┘
  │
  └── enrollments ────── students
```

### Key Relationships
- **Course → Modules**: One-to-Many
- **Module → Assignments**: One-to-Many
- **Student ↔ Courses**: Many-to-Many (via enrollments)
- **Assignment → Submissions**: One-to-Many

---

## 🔐 Security

- BCrypt password encoding
- Role-based access control (ADMIN, INSTRUCTOR, STUDENT)
- Session-based authentication
- CSRF protection (enabled for web, disabled for API)
- Secure file upload with validation

---

## 📧 Email Configuration

For development, MailHog captures emails without sending:

```bash
# MailHog UI
http://localhost:8025
```

For production, configure SMTP in `application-prod.yml`:

```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: your-email@gmail.com
    password: your-app-password
```

---

## 🐳 Docker Commands

```bash
# Build and start all services
docker-compose up --build -d

# View logs
docker-compose logs -f app

# Stop all services
docker-compose down

# Stop and remove volumes (clean database)
docker-compose down -v

# Access MySQL container
docker exec -it educationapi_db mysql -uroot -psecret
```

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgments

- Apple Design Inspiration for the dark mode UI
- Spring Boot team for the amazing framework
- Thymeleaf for seamless server-side templating

---

<div align="center">
  Made with ❤️ for education
</div>


---

