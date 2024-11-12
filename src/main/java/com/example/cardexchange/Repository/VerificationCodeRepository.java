package com.example.cardexchange.Repository;

import com.example.cardexchange.Entity.VerificationCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VerificationCodeRepository extends JpaRepository<VerificationCodeEntity, String> {
    void deleteByExpiryTimeBefore(Long currentTime);

    VerificationCodeEntity findByEmailAndCode(String email, String code);
}
