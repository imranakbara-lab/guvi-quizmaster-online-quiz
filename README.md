# QuizMaster – Online Quiz Application

![CI](https://github.com/imranakbara-lab/guvi-quizmaster-online-quiz/actions/workflows/ci.yml/badge.svg)

**Live:** https://guvi-quizmaster-online-quiz.vercel.app · **API docs:** https://quizmaster-api-es51.onrender.com/swagger-ui.html

Participants register, take timed multiple-choice quizzes, get scored instantly and review their past results. Admins create and manage quizzes and questions. Secure login with JWT and role-based access (ADMIN / PARTICIPANT), plus email notifications.

| Layer | Tech |
|---|---|
| Backend | Spring Boot 3, Spring Data JPA, REST, Spring Security + JWT, Hibernate Validator, `@ControllerAdvice`, Spring Boot Starter Mail, SpringDoc OpenAPI / Swagger |
| Frontend | React 18 (Vite), React Router, Axios, Bootstrap 5, Formik + Yup |
| Database | H2 (local) · PostgreSQL / MySQL (deployment) |
| Testing | JUnit 5 + Mockito (controller, service, repository) + end-to-end workflow test |

## Features

**Authentication & roles** – register / login with JWT; new users are PARTICIPANTs, the first ADMIN is created on start-up and admins can promote users. Forgot-password flow with an emailed reset link (valid 30 minutes).

**Question management (Admin)** – create, edit, publish / unpublish and delete quizzes (title, description, time limit). Add, edit and delete multiple-choice questions with 2–8 options; mark one or more options as correct. A quiz can only be published once it has questions.

**Quiz taking (Participant)** – browse and search published quizzes, take a quiz one question at a time with a question palette, single- or multi-select answers, a live countdown timer and auto-submit when time runs out. Refreshing the page resumes the same attempt with the remaining time (the timer is enforced on the server).

**Submission & scoring** – answers are graded on the server (a question is correct only when the selected options exactly match the correct ones). The attempt (user, quiz, answers, score, start/submit time, timed-out flag) is saved. Participants see their score, a full answer review and their result history; admins see all results and a per-quiz leaderboard.

**Email notifications** – registration confirmation, quiz result, password reset. Sent asynchronously with Spring Boot Starter Mail; when SMTP is not configured the email is written to the log instead.

## Project structure

```
quizmaster/
├── backend/   Spring Boot REST API
│   └── src/main/java/com/quizmaster/
│       ├── config/       Security, Swagger, Async, Clock, DataSeeder (admin + sample quizzes)
│       ├── controller/   Auth, Quiz, Attempt, Admin, User
│       ├── dto/          request / response records
│       ├── entity/       User, Quiz, Question, AnswerOption, QuizAttempt, AttemptAnswer, PasswordResetToken
│       ├── exception/    GlobalExceptionHandler (@RestControllerAdvice)
│       ├── repository/   Spring Data JPA repositories
│       ├── security/     JwtService, JwtAuthFilter, UserPrincipal
│       └── service/      AuthService, QuizService, AttemptService, EmailService, UserService
└── frontend/  React app (Vite)
```

## Run locally

```bash
# Backend (Java 17+, Maven) – http://localhost:8080 , Swagger at /swagger-ui.html
cd backend
mvn spring-boot:run
mvn test            # 50 tests: controller, service, repository and end-to-end

# Frontend (Node 18+) – http://localhost:5173
cd frontend
npm install
npm run dev
```
Default admin: `admin@quizmaster.com` / `Admin@123`. Two sample quizzes (Java Basics, Spring Boot Fundamentals) are created on first start.

## Environment variables (backend)

| Variable | Purpose |
|---|---|
| `DATABASE_URL` or `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | database connection |
| `DB_SCHEMA` | database schema for the tables (default `PUBLIC`) |
| `JWT_SECRET` | at least 32 characters |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | first admin account |
| `CORS_ORIGINS` | allowed frontend URL(s) |
| `FRONTEND_URL` | used for links inside emails |
| `MAIL_ENABLED`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` | SMTP (e.g. Gmail with an App Password) |

## API overview

| Method | Endpoint | Access |
|---|---|---|
| POST | `/api/auth/register`, `/api/auth/login`, `/api/auth/forgot-password`, `/api/auth/reset-password` | Public |
| GET | `/api/quizzes` | Public |
| POST | `/api/quizzes/{id}/start` | Logged in |
| POST | `/api/attempts/{id}/submit` | Owner |
| GET | `/api/attempts/my`, `/api/attempts/{id}` | Owner / Admin |
| GET / POST / PUT / DELETE | `/api/admin/quizzes`, `/api/admin/quizzes/{id}` | Admin |
| POST | `/api/admin/quizzes/{id}/questions` | Admin |
| PUT / DELETE | `/api/admin/questions/{id}` | Admin |
| GET | `/api/admin/results?quizId=` | Admin |
| GET / PUT / DELETE | `/api/admin/users`, `/api/admin/users/{id}` | Admin |

## Deployment
- **Backend → Render** (Docker, `render.yaml` blueprint) – https://quizmaster-api-es51.onrender.com
- **Frontend → Vercel** (root `frontend`, env `VITE_API_URL`) – https://guvi-quizmaster-online-quiz.vercel.app

### Demo logins
| Role | Email | Password |
|---|---|---|
| Admin | admin@quizmaster.com | Admin@123 |
| Participant | participant@quizmaster.com | Participant@123 |

> The backend runs on Render's free plan, so the first request after inactivity can take ~50 seconds while it wakes up.
