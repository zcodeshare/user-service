package com.in2it.cats.userservice.controller;

import com.in2it.cats.userservice.dto.ResponseDTO;
import com.in2it.cats.userservice.dto.UserRequestDTO;
import com.in2it.cats.userservice.dto.UserResponseDTO;
import com.in2it.cats.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "${user.create}")
    @PostMapping("/create")
    public ResponseEntity<ResponseDTO> createUser(@Valid @RequestBody UserRequestDTO request) {

        UserResponseDTO data = userService.createUser(request);
        ResponseDTO response = new ResponseDTO(true, data, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "${user.getById}")
    @GetMapping("/{id}")
    public ResponseEntity<ResponseDTO> getUserById(@PathVariable String id) {

        UserResponseDTO data = userService.getUserById(id);
        ResponseDTO response = new ResponseDTO(true, data, null);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "${user.getAll}")
    @GetMapping("/getAll")
    public ResponseEntity<ResponseDTO> getAllUsers() {

        List<UserResponseDTO> data = userService.getAllUsers();
        ResponseDTO response = new ResponseDTO(true, data, null);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "${user.update}")
    @PutMapping("/update/{id}")
    public ResponseEntity<ResponseDTO> updateUser(@PathVariable String id, @Valid @RequestBody UserRequestDTO request) {

        UserResponseDTO data = userService.updateUser(id, request);
        ResponseDTO response = new ResponseDTO(true, data, null);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "${user.delete}")
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ResponseDTO> deleteUser(@PathVariable String id) {

        userService.deleteUser(id);
        ResponseDTO response = new ResponseDTO(true, null, null);
        return ResponseEntity.ok(response);
    }
}