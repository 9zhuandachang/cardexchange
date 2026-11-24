package com.example.cardexchange.Repository;
import com.example.cardexchange.Entity.ProductCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CardProductRepository extends JpaRepository<ProductCard, Integer> {
    @Query("SELECT CAST(pc.id AS long) FROM ProductCard pc ORDER BY pc.money DESC")
    List<Long> findTop4CardIdsByMoney();
}
