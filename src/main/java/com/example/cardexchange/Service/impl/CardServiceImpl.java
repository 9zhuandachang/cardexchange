package com.example.cardexchange.Service.impl;

import com.example.cardexchange.Entity.Card;
import com.example.cardexchange.Entity.EsportsCards;
import com.example.cardexchange.Repository.CardRepository;
import com.example.cardexchange.Repository.EsportsCardRepository;
import com.example.cardexchange.Service.CardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CardServiceImpl implements CardService {
    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private EsportsCardRepository esportsCardRepository;

    @Override
    public List<String> searchByTeam(String team) {
        // 查询export表获得主键
        List<EsportsCards> byTeam = esportsCardRepository.findByTeam(team);
        List<Long> cardIds = byTeam.stream().map(EsportsCards::getId).collect(Collectors.toList());
        if(cardIds.isEmpty()){
            return Collections.emptyList();
        }

        // 根据主键查询卡片信息
        List<Card> cards = cardRepository.findAllById(cardIds);
        List<String> urls = cards.stream().map(Card::getImg_url).collect(Collectors.toList());
        return urls;
    }
}
