package com.example.cardexchange.Service;

import com.example.cardexchange.Entity.User;
import com.example.cardexchange.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;
    public User registerUser(User user) {
        // 可以在这里添加业务逻辑，比如检查用户是否已存在

        boolean isCodeValid = emailService.verifyCode(user.getEmail(), user.getVerificationCode());

        if (!isCodeValid) {
            throw new IllegalArgumentException("code is wrong");
        }


        if (userRepository.findByUsername(user.getname()) != null) {
            throw new IllegalArgumentException("Username is already taken");
        }

        userRepository.save(user);
        return user;
    }
    public boolean userlogin(User user)
    {
       String name = user.getname();
       String password=user.getPassword();
        User true_user=userRepository.findByUsername(user.getname());
        if(!true_user.getPassword().equals(password))
        {
            throw new IllegalArgumentException("password wrong");
        }
        return true;
    }

    public User getUserByUsername(String username) {
        User true_user=userRepository.findByUsername(username);
        return true_user;
    }
}