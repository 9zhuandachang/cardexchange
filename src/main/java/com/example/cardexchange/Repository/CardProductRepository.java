package com.example.cardexchange.Repository;
import com.example.cardexchange.Entity.ProductCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CardProductRepository extends JpaRepository<ProductCard, Long> {
    @Query("SELECT pc.id FROM product_card pc ORDER BY pc.money DESC")
    List<Long> findTop4CardIdsByMoney();
}
