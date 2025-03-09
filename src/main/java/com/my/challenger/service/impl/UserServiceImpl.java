//package com.my.challenger.service.impl;
//
//import com.my.challenger.dto.LoginRequest;
//import com.my.challenger.dto.UserDTO;
//import com.my.challenger.entity.User;
//import com.my.challenger.lang.Endpoint;
//import com.my.challenger.mapper.UserMapper;
//import com.my.challenger.repository.UserRepository;
//import lombok.AllArgsConstructor;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.Optional;
//
//@AllArgsConstructor
//@RestController
//@RequestMapping("/api/v1/user")
//public class UserServiceImpl {
//
//    private final UserRepository userRepository;
//    private final BCryptPasswordEncoder passwordEncoder;
//
////    @PostMapping("/add-new-user")
////    public ResponseEntity<User> newEmployee(@RequestBody User newEmployee) {
////        // Hash the password before saving
////        newEmployee.setPassword(newEmployee.getPassword(), passwordEncoder);
////        User savedUser = userRepository.save(newEmployee);
////        return ResponseEntity.ok(savedUser);
////    }
//
//
//    @GetMapping(Endpoint.UI.V1.User.GET_USER)
//    public ResponseEntity<UserDTO> getUsers(@RequestParam String name) {
//        Optional<User> byUserName = userRepository.findByUserName(name);
//        UserDTO userDTO = byUserName.map(UserMapper.INSTANCE::toUserDTO).orElse(null);
//        return ResponseEntity.ok(userDTO);
//    }
//
//    @PostMapping("/login")
//    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
//        Optional<User> userOptional = userRepository.findByUserName(loginRequest.getUserName());
//
//        if (userOptional.isPresent()) {
//            User user = userOptional.get();
//            // Verify the password
//            if (user.isPasswordMatch(loginRequest.getPassword(), passwordEncoder)) {
//                return ResponseEntity.ok("Login successful");
//            }
//        }
//
//        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
//    }
//}