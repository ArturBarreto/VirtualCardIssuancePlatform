package com.nium.cardplatform.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@RestController
public class HelloController {
    @GetMapping("/")
    public String home() {
        return """
            <h2>Hello, Virtual Card Platform is running!</h2>
            <ul>
                <li><b>API Documentation (Swagger):</b> <a href="/swagger-ui/index.html" target="_blank">/swagger-ui/index.html</a></li>
                <li><b>H2 Database Console:</b> <a href="/h2-console" target="_blank">/h2-console</a></li>
            </ul>
            <p><b>H2 DB Credentials:</b><br><br>
            JDBC URL: <code>jdbc:h2:mem:testdb</code><br>
            Username: <code>sa</code><br>
            Password: <code>(empty)</code>
            </p>
        """;
    }
}
