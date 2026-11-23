package com.quizmaster.config;

import com.quizmaster.entity.*;
import com.quizmaster.repository.QuizRepository;
import com.quizmaster.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Creates the first Admin account and two sample quizzes on the first start. */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final QuizRepository quizRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;
    @Value("${app.admin.password}")
    private String adminPassword;
    @Value("${app.seed-sample-quizzes:true}")
    private boolean seedQuizzes;

    public DataSeeder(UserRepository userRepository, QuizRepository quizRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.quizRepository = quizRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!userRepository.existsByEmail(adminEmail)) {
            userRepository.save(new User("Admin", adminEmail, passwordEncoder.encode(adminPassword), null, Role.ADMIN));
            log.info("Default admin created: {}", adminEmail);
        }
        if (seedQuizzes && quizRepository.count() == 0) {
            quizRepository.save(javaQuiz());
            quizRepository.save(springQuiz());
            log.info("Sample quizzes created");
        }
    }

    private Quiz javaQuiz() {
        Quiz q = quiz("Java Basics", "Test your knowledge of core Java concepts.", 5);
        add(q, "Which keyword is used to inherit a class in Java?", 1, "implements", "extends", "inherits", "super");
        add(q, "What is the default value of an int field in a Java class?", 0, "0", "null", "1", "undefined");
        add(q, "Which collection does NOT allow duplicate elements?", 2, "ArrayList", "LinkedList", "HashSet", "Vector");
        add(q, "Which method is the entry point of a Java application?", 3, "start()", "run()", "init()", "main()");
        add(q, "Which of these are primitive types? (choose all that apply)", new int[]{0, 2}, "int", "String", "boolean", "Integer");
        return q;
    }

    private Quiz springQuiz() {
        Quiz q = quiz("Spring Boot Fundamentals", "REST APIs, dependency injection and JPA.", 5);
        add(q, "Which annotation marks a class as a REST controller?", 2, "@Controller", "@Service", "@RestController", "@Component");
        add(q, "Which annotation injects a request body into a method parameter?", 1, "@PathVariable", "@RequestBody", "@RequestParam", "@ModelAttribute");
        add(q, "Which interface gives CRUD methods for a JPA entity?", 0, "JpaRepository", "CrudService", "EntityManagerFactory", "DataSource");
        add(q, "Where are Spring Boot settings usually kept?", 3, "web.xml", "pom.xml", "beans.xml", "application.properties");
        return q;
    }

    private static Quiz quiz(String title, String description, int minutes) {
        Quiz q = new Quiz();
        q.setTitle(title);
        q.setDescription(description);
        q.setTimeLimitMinutes(minutes);
        q.setPublished(true);
        return q;
    }

    private static void add(Quiz quiz, String text, int correctIndex, String... options) {
        add(quiz, text, new int[]{correctIndex}, options);
    }

    private static void add(Quiz quiz, String text, int[] correct, String... options) {
        Question question = new Question();
        question.setText(text);
        question.setPosition(quiz.getQuestions().size() + 1);
        for (int i = 0; i < options.length; i++) {
            final int idx = i;
            question.addOption(new AnswerOption(options[i], java.util.Arrays.stream(correct).anyMatch(c -> c == idx)));
        }
        quiz.addQuestion(question);
    }
}
