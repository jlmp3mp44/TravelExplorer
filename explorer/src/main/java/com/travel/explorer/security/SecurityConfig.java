package com.travel.explorer.security;

import com.travel.explorer.entities.AppRole;
import com.travel.explorer.entities.Role;
import com.travel.explorer.entities.User;
import com.travel.explorer.repo.RoleRepository;
import com.travel.explorer.repo.UserRepository;
import com.travel.explorer.security.jwt.AuthEntryPointJwt;
import com.travel.explorer.security.jwt.AuthTokenFilter;
import com.travel.explorer.security.service.UserDetailServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
//import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


import java.util.Set;

@Configuration
@EnableWebSecurity
//@EnableMethodSecurity
public class SecurityConfig {
  @Autowired
  UserDetailServiceImpl userDetailsService;

  @Autowired
  private AuthEntryPointJwt unauthorizedHandler;

  @Bean
  public AuthTokenFilter authenticationJwtTokenFilter() {
    return new AuthTokenFilter();
  }


  @Bean
  public DaoAuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();

    authProvider.setUserDetailsService(userDetailsService);
    authProvider.setPasswordEncoder(passwordEncoder());

    return authProvider;
  }


  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
    return authConfig.getAuthenticationManager();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }



  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth ->
            auth.requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/v3/api-docs/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                //.requestMatchers("/api/admin/**").permitAll()
                //.requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/api/test/**").permitAll()
                .requestMatchers("/images/**").permitAll()
                .anyRequest().authenticated()
        );

    http.authenticationProvider(authenticationProvider());

    http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);
    http.headers(headers -> headers.frameOptions(
        frameOptions -> frameOptions.sameOrigin()));

    return http.build();
  }

  @Bean
  public WebSecurityCustomizer webSecurityCustomizer() {
    return (web -> web.ignoring().requestMatchers("/v2/api-docs",
        "/configuration/ui",
        "/swagger-resources/**",
        "/configuration/security",
        "/swagger-ui.html",
        "/webjars/**"));
  }


  @Bean
  public CommandLineRunner initData(RoleRepository roleRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
    return args -> {
      // Retrieve or create roles
      Role userRole = roleRepository.findByRoleName(AppRole.ROLE_USER)
          .orElseGet(() -> {
            Role newUserRole = new Role(AppRole.ROLE_USER);
            return roleRepository.save(newUserRole);
          });


      Role adminRole = roleRepository.findByRoleName(AppRole.ROLE_ADMIN)
          .orElseGet(() -> {
            Role newAdminRole = new Role(AppRole.ROLE_ADMIN);
            return roleRepository.save(newAdminRole);
          });

      Set<Role> userRoles = Set.of(userRole);
      Set<Role> adminRoles = Set.of(userRole, adminRole);


      // Create users if not already present
      if (!userRepository.existsByUsername("user1")) {
        User user1 = new User("user1", "user1@example.com", passwordEncoder.encode("password1"), userRoles);
        userRepository.save(user1);
      }

      if (!userRepository.existsByUsername("admin")) {
        User admin = new User("admin", "admin@example.com", passwordEncoder.encode("adminPass"), adminRoles);
        userRepository.save(admin);
      }
    };
  }

}