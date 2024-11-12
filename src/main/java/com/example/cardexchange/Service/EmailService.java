package com.example.cardexchange.Service;
import com.example.cardexchange.Entity.VerificationCodeEntity;
import com.example.cardexchange.Repository.VerificationCodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import org.springframework.mail.javamail.JavaMailSender;


@Service
public class EmailService {

    @Autowired
    private VerificationCodeRepository repository;

    @Autowired
    private JavaMailSender mailSender; // Ensure mailSender is injected

    public void sendVerificationCode(String email) {
        String code = generateVerificationCode();
        VerificationCodeEntity verificationCode = new VerificationCodeEntity();
        verificationCode.setEmail(email);
        verificationCode.setCode(code);
        verificationCode.setExpiryTime(LocalDateTime.now().plus(5, ChronoUnit.MINUTES)); // 5分钟有效期
        repository.save(verificationCode);

        // Define subject and text for the email
        String subject = "Your Verification Code";
        String text = "Your verification code is: " + code + ". It will expire in 5 minutes.";

        // 调用邮件服务发送验证码
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("3481044089@qq.com");
        message.setTo(email);
        message.setSubject(subject);
        message.setText(text);

        try {
            mailSender.send(message);
        } catch (Exception e) {
            // Handle the error appropriately
            System.err.println("Failed to send email: " + e.getMessage());
        }
    }

    private String generateVerificationCode() {
        Random random = new Random();
        return Integer.toString(random.nextInt(900000) + 100000);
    }

    public boolean verifyCode(String email, String code) {
        VerificationCodeEntity verificationCode = repository.findByEmailAndCode(email, code);

        if (verificationCode == null) {
            return false; // Invalid code
        }

        if (verificationCode.getExpiryTime().isBefore(LocalDateTime.now())) {
            return false; // Code has expired
        }

        return true; // Code is valid
    }
}
