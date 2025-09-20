package com.paulpladziewicz.fremontmi.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import com.paulpladziewicz.fremontmi.user.CustomUserDetailsService;
import com.paulpladziewicz.fremontmi.user.UserRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserRepository userRepository;

    public SecurityConfig(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests((authorize) -> authorize
                .requestMatchers(HttpMethod.POST, "/api/vectors/search", "/api/upload", "/register", "/contact",
                        "/subscribe", "/api/stripe/**", "/article-contact", "/taqueria",
                        "/contact/neighbor-services-profile")
                .permitAll()
                .requestMatchers("/search", "/about", "/api/subscribe", "/api/events", "/overview/**", "/articles/**",
                        "/groups/", "/groups/**", "/events", "/events/**", "/register", "/forgot-password",
                        "/reset-password", "/forgot-username", "/css/**", "/privacy-policy", "/terms-of-service",
                        "/js/**", "/images/**", "/favicon.ico", "/error", "/login", "/login?error", "/login?error=*",
                        "/login?logout", "/businesses", "/businesses/**", "/neighbor-services", "/neighbor-services/**",
                        "/confirm", "/tagging-guidelines", "/create/neighbor-services-profile/overview",
                        "/create/business/overview", "/", "/robots.txt", "/sitemap.xml", "/health", "/group/**",
                        "/event/**", "/business/**", "/neighbor-services-profile/**")
                .permitAll()
                .anyRequest().authenticated());
        http.formLogin(formLogin -> formLogin
                .loginPage("/login")
                .permitAll()
                .failureHandler(customAuthenticationFailureHandler())
                .successHandler(customSavedRequestAwareAuthenticationSuccessHandler()));
        http.requestCache(requestCacheCustomizer -> requestCacheCustomizer
                .requestCache(new CustomRequestCache()));
        http.logout(logout -> logout
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll());
        http.csrf((csrf) -> csrf
                .ignoringRequestMatchers("/login"));
        return http.build();
    }

    @Bean
    public CustomSavedRequestAwareAuthenticationSuccessHandler customSavedRequestAwareAuthenticationSuccessHandler() {
        return new CustomSavedRequestAwareAuthenticationSuccessHandler(userRepository);
    }

    @Bean
    public AuthenticationFailureHandler customAuthenticationFailureHandler() {
        return new CustomAuthenticationFailureHandler(userRepository);
    }

    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder);

        return new ProviderManager(authenticationProvider);
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return new CustomUserDetailsService(userRepository);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
