package com.bank.bank_system.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.core.annotation.Order;

@Configuration
public class SecurityConfiguration {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager customerAuthenticationManager(
            CustomerUserDetailsService customerUserDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(customerUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    SecurityContextRepository customerSecurityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    @Order(1)
    SecurityFilterChain customerSecurityFilterChain(
            HttpSecurity http,
            AuthenticationManager customerAuthenticationManager,
            SecurityContextRepository customerSecurityContextRepository) throws Exception {
        http.securityMatcher("/client/**", "/api/customer/**", "/login/client", "/register", "/terms",
                        "/client-login.html", "/register.html", "/logout/client", "/csrf", "/css/**", "/js/**")
                .authenticationManager(customerAuthenticationManager)
                .securityContext(context -> context.securityContextRepository(customerSecurityContextRepository))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/login/client", "/register", "/terms", "/client-login.html", "/register.html", "/csrf", "/css/**", "/js/**").permitAll()
                        .requestMatchers("/api/customer/**", "/client/**").hasRole("CUSTOMER")
                        .anyRequest().authenticated())
                .formLogin(form -> form.loginPage("/login/client")
                        .loginProcessingUrl("/login/client")
                        .defaultSuccessUrl("/client/dashboard", true)
                        .failureUrl("/login/client?error"))
                .logout(logout -> logout.logoutUrl("/logout/client").logoutSuccessUrl("/login/client?logout"));
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain employeeSecurityFilterChain(
            HttpSecurity http,
            EmployeeUserDetailsService employeeUserDetailsService,
            PasswordEncoder passwordEncoder) throws Exception {
        DaoAuthenticationProvider employeeAuthentication = new DaoAuthenticationProvider(employeeUserDetailsService);
        employeeAuthentication.setPasswordEncoder(passwordEncoder);
        http.authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/login", "/login/employee", "/login.html", "/employee-login.html", "/csrf", "/error", "/css/**", "/js/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/employee/membership-requests/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/api/employee/**").hasAnyRole("ADMIN", "MANAGER", "OPERATOR")
                        .requestMatchers("/", "/index.html", "/employee/**").hasAnyRole("ADMIN", "MANAGER", "OPERATOR")
                        .requestMatchers("/api/reports/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/api/**").hasAnyRole("ADMIN", "MANAGER", "OPERATOR")
                        .anyRequest().authenticated())
                .authenticationProvider(employeeAuthentication)
                .formLogin(form -> form.loginPage("/login/employee")
                        .loginProcessingUrl("/login/employee")
                        .defaultSuccessUrl("/employee/dashboard", true)
                        .failureUrl("/login/employee?error").permitAll())
                .logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/login/employee?logout"));
        return http.build();
    }
}
