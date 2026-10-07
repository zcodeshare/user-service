package com.in2it.cats.userservice.service;

import com.in2it.cats.userservice.dto.UserRequestDTO;
import com.in2it.cats.userservice.dto.UserResponseDTO;

import java.util.List;

public interface UserService {

    UserResponseDTO createUser(UserRequestDTO request);
    UserResponseDTO getUserById(String id);
    List<UserResponseDTO> getAllUsers();
    UserResponseDTO updateUser(String id, UserRequestDTO request);
    void deleteUser(String id);
}