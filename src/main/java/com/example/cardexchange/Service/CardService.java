package com.example.cardexchange.Service;
import com.example.cardexchange.Entity.Card;
import com.example.cardexchange.Pojo.CardVo;

import java.util.List;

public interface CardService {
    List<Card> searchByTeam(String team);

    CardVo searchById(Long id);

    List<CardVo> searchRecomendtions();


}
