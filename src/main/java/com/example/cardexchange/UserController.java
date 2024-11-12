package com.example.cardexchange;

import com.example.cardexchange.Entity.User;
import com.example.cardexchange.Service.EmailService;
import com.example.cardexchange.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private EmailService emailService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {

        try {

            User registeredUser = userService.registerUser(user);

            return ResponseEntity.ok(registeredUser);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/sendcode")
    public ResponseEntity<?> sendVerificationCode(@RequestParam String email) {
        // 调用 EmailService 发送验证码
        emailService.sendVerificationCode(email);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        return ResponseEntity.ok(response);
    }
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String username,@RequestParam String password)
    {
        try {
            User new_user = new User();
            new_user.setUsername(username);
            new_user.setPassword(password);
            boolean verify = userService.userlogin(new_user);
            if (verify) {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "Login successful");
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "error");
                response.put("message", "Invalid username or password");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

}
