package com.example.cardexchange.Entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.LocalDateTime;

@Entity
public class VerificationCodeEntity {
    @Id
    private String email;
    private String code;
    private LocalDateTime expiryTime;

    public String getEmail() {
        return email;
    }

    public LocalDateTime getExpiryTime() {
        return expiryTime;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setExpiryTime(LocalDateTime expiryTime) {
        this.expiryTime = expiryTime;
    }
}
