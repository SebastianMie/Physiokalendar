package com.example.physiokalendar.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import static org.springframework.security.config.Customizer.withDefaults;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/api/auth/**").permitAll() // Erlaubt Zugriff auf Authentifizierungsendpunkte
                .anyRequest().authenticated() // Alle anderen Anfragen erfordern Authentifizierung
            )
            .formLogin(withDefaults()) // Falls du eine benutzerdefinierte Login-Seite verwenden möchtest
            .csrf(csrf -> csrf.disable()); // Falls du CSRF-Schutz deaktivieren möchtest (nicht empfohlen für Produktionsumgebungen)
        
        return http.build();
    }
}