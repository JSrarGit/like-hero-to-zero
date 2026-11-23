package de.likeherotozero.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;

@Configuration
public class SecurityConfiguration {
    @Bean
    SecurityFilterChain applicationSecurity(HttpSecurity http) {
        return http.authorizeHttpRequests(access -> access
                        .requestMatchers("/", "/login", "/css/**", "/error").permitAll()
                        .requestMatchers("/forschung", "/forschung/**").hasRole("SCIENTIST")
                        .anyRequest().denyAll())
                .formLogin(login -> login.loginPage("/login").defaultSuccessUrl("/forschung", true).permitAll())
                .logout(logout -> logout.logoutSuccessUrl("/?abgemeldet"))
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService researchAccount(PasswordEncoder encoder,
                                       @Value("${app.research.username}") String username,
                                       @Value("${app.research.password}") String password) {
        InMemoryUserDetailsManager accounts = new InMemoryUserDetailsManager();
        if (StringUtils.hasText(password)) {
            accounts.createUser(User.withUsername(username).password(encoder.encode(password))
                    .roles("SCIENTIST").build());
        }
        return accounts;
    }
}
