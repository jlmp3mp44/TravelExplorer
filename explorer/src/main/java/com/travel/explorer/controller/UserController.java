package com.travel.explorer.controller;

import com.travel.explorer.security.responce.UserInfoResponse;
import com.travel.explorer.security.service.UserDetailsImpl;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class UserController {

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

    UserInfoResponse response = new UserInfoResponse(userDetails.getId(),
        userDetails.getUsername(), userDetails.getEmail(), roles);

    return ResponseEntity.ok().body(response);
  }


}
