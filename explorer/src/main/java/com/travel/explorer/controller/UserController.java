package com.travel.explorer.controller;

import com.travel.explorer.entities.User;
import com.travel.explorer.repo.UserRepository;
import com.travel.explorer.security.request.UserProfileUpdateRequest;
import com.travel.explorer.security.responce.UserInfoResponse;
import com.travel.explorer.security.service.UserDetailsImpl;
import com.travel.explorer.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class UserController {

  @Autowired
  UserService userService;

  @Autowired
  UserRepository userRepository;

  @GetMapping("/username")
  public String currentUsername(Authentication authentication){
    if (authentication!=null){
      return authentication.getName();
    }
    else return "";
  }

  @GetMapping("/user")
  public ResponseEntity<UserInfoResponse> getUserDetails(Authentication authentication){

    UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

    List<String> roles = userDetails.getAuthorities().stream()
        .map(item -> item.getAuthority())
        .collect(Collectors.toList());

    User user =
        userRepository
            .findById(userDetails.getId())
            .orElseThrow(() -> new IllegalStateException("User not found"));

    UserInfoResponse response =
        new UserInfoResponse(
            user.getUserId(),
            user.getUsername(),
            user.getEmail(),
            user.getPhoneNumber(),
            roles);

    return ResponseEntity.ok().body(response);
  }

  @PutMapping("/user")
  public ResponseEntity<UserInfoResponse> updateProfile(
      @Valid @RequestBody UserProfileUpdateRequest request, Authentication authentication) {
    UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
    User user =
        userRepository
            .findById(userDetails.getId())
            .orElseThrow(() -> new IllegalStateException("User not found"));
    if (request.getPhoneNumber() != null) {
      String p = request.getPhoneNumber().trim();
      user.setPhoneNumber(p.isEmpty() ? null : p);
    }
    userRepository.save(user);

    List<String> roles =
        userDetails.getAuthorities().stream()
            .map(item -> item.getAuthority())
            .collect(Collectors.toList());

    UserInfoResponse response =
        new UserInfoResponse(
            user.getUserId(),
            user.getUsername(),
            user.getEmail(),
            user.getPhoneNumber(),
            roles);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/admin/users")
  public ResponseEntity<List<User>> getAllUsers(){
    return new ResponseEntity<>(userService.getAllUsers(), HttpStatus.OK);
  }


}
