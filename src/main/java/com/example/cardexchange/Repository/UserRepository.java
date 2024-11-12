package com.example.cardexchange.Repository;

import com.example.cardexchange.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // 可以添加一些自定义的数据库查询方法，例如检查用户名是否已存在
    User findByUsername(String username);
}