package org.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http
    ) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth

                        /*
                         * Veřejně dostupné části aplikace.
                         */
                        .requestMatchers(
                                "/",
                                "/produkty/**",
                                "/kosik/**",
                                "/images/**",
                                "/invoices/**",
                                "/login",
                                "/registrace",
                                "/css/**",
                                "/js/**",
                                "/error",
                                "/o-nas",
                                "/kontakt"
                        )
                        .permitAll()

                        /*
                         * Části administrace dostupné pouze
                         * uživatelům s rolí ADMIN.
                         *
                         * Specifičtější pravidla musejí být uvedena
                         * před obecným pravidlem /admin/**.
                         */
                        .requestMatchers(
                                "/admin/installation-posts/**",
                                "/admin/uzivatele/**",
                                "/admin/logs/**",
                                "/admin/dph/**"
                        )
                        .hasRole("ADMIN")

                        /*
                         * Ostatní administrační části mohou používat
                         * administrátoři i zaměstnanci.
                         */
                        .requestMatchers("/admin/**")
                        .hasAnyRole(
                                "ADMIN",
                                "EMPLOYEE"
                        )

                        /*
                         * Docházka zaměstnanců.
                         */
                        .requestMatchers("/dochazka/**")
                        .hasAnyRole(
                                "EMPLOYEE",
                                "ADMIN"
                        )

                        /*
                         * Zákaznický účet.
                         */
                        .requestMatchers("/muj-ucet/**")
                        .hasRole("CUSTOMER")

                        /*
                         * Všechny ostatní požadavky vyžadují
                         * přihlášeného uživatele.
                         */
                        .anyRequest()
                        .authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutRequestMatcher(
                                new AntPathRequestMatcher(
                                        "/logout"
                                )
                        )
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