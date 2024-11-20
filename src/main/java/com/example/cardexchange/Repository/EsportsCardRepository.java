package com.example.cardexchange.Repository;
import com.example.cardexchange.Entity.EsportsCards;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EsportsCardRepository extends JpaRepository<EsportsCards, Long> {
    @Query("SELECT e FROM EsportsCards e WHERE e.team = :team")
    List<EsportsCards> findByTeam(@Param("team") String team);
}
