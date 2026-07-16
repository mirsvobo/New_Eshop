package org.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/produkty/**", "/kosik/**", "/images/**", "/invoices/**",
                                "/login", "/registrace", "/css/**", "/js/**", "/error", "/o-nas", "/kontakt").permitAll()

                        .requestMatchers("/admin/uzivatele/**", "/admin/logs/**", "/admin/dph/**").hasRole("ADMIN")

                        .requestMatchers("/admin/**").hasAnyRole("ADMIN", "EMPLOYEE")

                        .requestMatchers("/dochazka/**").hasAnyRole("EMPLOYEE", "ADMIN")

                        .requestMatchers("/muj-ucet/**").hasRole("CUSTOMER")

                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                        .logoutSuccessUrl("/")
                        .permitAll()
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}