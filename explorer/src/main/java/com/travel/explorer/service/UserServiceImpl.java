package com.travel.explorer.service;

import com.travel.explorer.entities.User;
import com.travel.explorer.repo.UserRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService{
  @Autowired
  UserRepository userRepository;


  @Override
  public List<User> getAllUsers() {
    return userRepository.findAll();
  }
}
