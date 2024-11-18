package com.example.cardexchange.Service.impl;

import com.example.cardexchange.Entity.Card;
import com.example.cardexchange.Entity.EsportsCards;
import com.example.cardexchange.Pojo.CardVo;
import com.example.cardexchange.Repository.CardRepository;
import com.example.cardexchange.Repository.EsportsCardRepository;
import com.example.cardexchange.Service.CardService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CardServiceImpl implements CardService {
    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private EsportsCardRepository esportsCardRepository;

    @Override
    public List<CardVo> searchByTeam(String team) {
        // 查询export表获得主键
        List<EsportsCards> byTeam = esportsCardRepository.findByTeam(team);
        List<Long> cardIds = byTeam.stream().map(EsportsCards::getId).collect(Collectors.toList());
        if(cardIds.isEmpty()){
            return Collections.emptyList();
        }

        // 根据主键查询卡片信息
        List<Card> cards = cardRepository.findAllById(cardIds);
        Map<Long, Card> cardMap = cards.stream()
                .collect(Collectors.toMap(Card::getId, card -> card));

        List<CardVo> res = new ArrayList<>();
        for(EsportsCards esportsCards : byTeam){
            CardVo cardVo = new CardVo();
            BeanUtils.copyProperties(esportsCards, cardVo );
            cardVo.setImg_url(cardMap.get(esportsCards.getId()).getImg_url());
            res.add(cardVo);
        }
        return res;
    }
}
