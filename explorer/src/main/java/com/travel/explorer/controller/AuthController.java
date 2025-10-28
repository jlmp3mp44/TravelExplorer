package com.travel.explorer.controller;

import com.travel.explorer.entities.AppRole;
import com.travel.explorer.entities.Role;
import com.travel.explorer.entities.User;
import com.travel.explorer.repo.RoleRepository;
import com.travel.explorer.repo.UserRepository;
import com.travel.explorer.security.jwt.JwtUtils;
import com.travel.explorer.security.request.LoginRequest;
import com.travel.explorer.security.request.SignUpRequest;
import com.travel.explorer.security.responce.MessageResponce;
import com.travel.explorer.security.responce.UserInfoResponse;
import com.travel.explorer.security.service.UserDetailsImpl;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/auth")
public class AuthController {

  @Autowired
  private JwtUtils jwtUtils;

  @Autowired
  private AuthenticationManager authenticationManager;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private RoleRepository roleRepository;

  @PostMapping("/signin")
  public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
    Authentication authentication;
    try {
      authentication = authenticationManager
          .authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
    } catch (AuthenticationException exception) {
      Map<String, Object> map = new HashMap<>();
      map.put("message", "Bad credentials");
      map.put("status", false);
      return new ResponseEntity<Object>(map, HttpStatus.NOT_FOUND);
    }

    SecurityContextHolder.getContext().setAuthentication(authentication);

    UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

    ResponseCookie jwtCookie = jwtUtils.generateJwtCookie(userDetails);

    List<String> roles = userDetails.getAuthorities().stream()
        .map(item -> item.getAuthority())
        .collect(Collectors.toList());

    UserInfoResponse response = new UserInfoResponse(userDetails.getId(),
        userDetails.getUsername(), userDetails.getEmail(), roles);

    return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
        .body(response);
  }

  @PostMapping("/signup")
  public ResponseEntity<?> sighUpUser(@Valid  @RequestBody SignUpRequest signUpRequest) {

    if(userRepository.existsByUsername(signUpRequest.getUsername())){
      return ResponseEntity.badRequest().body(new MessageResponce("Username already taken!"));
    }

    if(userRepository.existsByEmail(signUpRequest.getEmail())){
      return ResponseEntity.badRequest().body(new MessageResponce("Email already taken!"));
    }

    Set<String> strRoles = signUpRequest.getRoles();

    Set<Role> roles =  new HashSet<>();

    if(strRoles==null){
      Role role = roleRepository.findByRoleName(AppRole.ROLE_USER)
          .orElseThrow(()-> new RuntimeException("Role is not found"));
      roles.add(role);
    }
    else {
      strRoles.forEach(role -> {
                switch (role){
                  case "admin":
                    Role adminRole = roleRepository.findByRoleName(AppRole.ROLE_ADMIN)
                        .orElseThrow(()-> new RuntimeException("Role is not found"));
                    roles.add(adminRole);
                    break;
                  default:
                    Role userRole = roleRepository.findByRoleName(AppRole.ROLE_USER)
                        .orElseThrow(()-> new RuntimeException("Role is not found"));
                    roles.add(userRole);
                }
              }
      );
    }
    User user =  new User(
        signUpRequest.getUsername(),
        signUpRequest.getEmail(),
        passwordEncoder.encode(signUpRequest.getPassword()),
        roles
    );

    userRepository.save(user);
    return ResponseEntity.ok(new MessageResponce("User registered successfully"));
  }

  @PostMapping("/signout")
  public ResponseEntity<?> signoutUser(){
    ResponseCookie cookie = jwtUtils.getCleanJwtCookie();

    return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString())
        .body(new MessageResponce("You have been signed out"));
  }
}
