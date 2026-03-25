package com.trip.expense_splitter.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Enable and configure CORS
            .csrf(csrf -> csrf.disable()) // 1. CSRF is disabled for stateless APIs
            .authorizeHttpRequests(auth -> auth
                // --- Allow access to the frontend ---
                .requestMatchers("/", "/index.html", "/*.css", "/*.js", "/favicon.ico", "/*.png", "/*.json").permitAll()

                // --- Allow access to public API endpoints ---
                .requestMatchers(HttpMethod.GET, "/api/users", "/api/expenses", "/api/settlements", "/api/spending-breakdown").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/users", "/api/expenses").permitAll() // 2. POST to /api/users is permitted

                // --- Secure all other endpoints ---
                .anyRequest().authenticated()
            )
            // Use stateless session management as we are using JWTs
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // For development, allowing the frontend origin is sufficient.
        // In production, you should be more restrictive.
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:8080"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Apply this CORS configuration to all API endpoints
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}