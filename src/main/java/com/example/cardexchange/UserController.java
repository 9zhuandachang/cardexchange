package com.example.cardexchange;
import com.example.cardexchange.Entity.User;
import com.example.cardexchange.Service.EmailService;
import com.example.cardexchange.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
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
    public ResponseMessage<User> register(@RequestBody User user) {

        try {

            User registeredUser = userService.registerUser(user);

            return ResponseMessage.success(registeredUser);

        } catch (IllegalArgumentException e) {
            return ResponseMessage.badRequestString(e.getMessage());
        }
    }

    @PostMapping("/sendcode")
    public ResponseMessage<Map<String, Object>> sendVerificationCode(@RequestParam String email) {
        // 调用 EmailService 发送验证码
        emailService.sendVerificationCode(email);
        Map<String, Object> response = new HashMap<>();
        return ResponseMessage.success(response);
    }
    @PostMapping("/login")
    public ResponseMessage<Map<String, Object>> login(@RequestParam String username, @RequestParam String password)
    {
        try {
            User newUser = new User();
            newUser.setUsername(username);
            newUser.setPassword(password);
            boolean verify = userService.userlogin(newUser);
            if (verify) {
                User user = userService.getUserByUsername(username);
                if (user != null) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("status", "success");
                    response.put("message", "Login successful");
                    response.put("user", user); // 将用户信息添加到响应中

                    return ResponseMessage.success(response);
                } else {
                    Map<String, Object> response = new HashMap<>();
                    response.put("status", "error");
                    response.put("message", "User not found");
                    return ResponseMessage.badRequest(response);
                }
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "error");
                response.put("message", "Invalid username or password");
                return ResponseMessage.unauthorized(response);
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseMessage.badRequest(response);
        }
    }
}
