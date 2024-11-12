package com.example.cardexchange.Repository;
import com.example.cardexchange.Entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {
    // 这里可以添加自定义查询方法，例如：
    // List<Card> findByName(String name);
    // Card findByImgUrl(String imgUrl);
}
