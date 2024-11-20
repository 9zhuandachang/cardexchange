package com.example.cardexchange.Repository;
import com.example.cardexchange.Entity.Card;
import com.example.cardexchange.Entity.EsportsCards;
import com.example.cardexchange.Entity.ProductCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductCardRepository extends JpaRepository<ProductCard, Long> {
    @Query("SELECT e FROM ProductCard e WHERE e.id = :id and e.userId = :userId")
    List<ProductCard> findByIdAndUserId(@Param("id") Integer id, @Param("userId") Long userId);

    @Transactional
    @Modifying
    @Query("UPDATE ProductCard e SET e.userId = :newUserId WHERE e.id = :id AND e.userId = :userId")
    void updateProductCard(@Param("id") Integer id, @Param("userId") Long userId, @Param("newUserId") Long newUserId);

}
