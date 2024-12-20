package com.example.cardexchange.Service.impl;

import com.example.cardexchange.Entity.Card;
import com.example.cardexchange.Entity.EsportsCards;
import com.example.cardexchange.Pojo.CardVo;
import com.example.cardexchange.Repository.CardProductRepository;
import com.example.cardexchange.Repository.CardRepository;
import com.example.cardexchange.Repository.EsportsCardRepository;
import com.example.cardexchange.Service.CardService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CardServiceImpl implements CardService {
    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private EsportsCardRepository esportsCardRepository;


    @Override
    public List<Card> searchByTeam(String team) {
        // 查询export表获得主键
        List<EsportsCards> byTeam = esportsCardRepository.findByTeam(team);
        List<Long> cardIds = byTeam.stream().map(EsportsCards::getId).collect(Collectors.toList());
        if(cardIds.isEmpty()){
            return Collections.emptyList();
        }

        // 根据主键查询卡片信息
        List<Card> cards = cardRepository.findAllById(cardIds);
        return cards;
    }

    @Override
    public CardVo searchById(Long id) {
        Optional<Card> optionalCard = cardRepository.findById(id);

        Optional<EsportsCards> optionalEsportsCards = esportsCardRepository.findById(id);

        CardVo cardVo = new CardVo();
        BeanUtils.copyProperties(optionalEsportsCards.get(), cardVo);
        cardVo.setImg_url(optionalCard.get().getImg_url());

        return cardVo;
    }

    @Autowired
    private CardProductRepository  cardProductRepository;
    public List<CardVo> searchRecomendtions()
    {
        List<Long> list= cardProductRepository.findTop4CardIdsByMoney();
        List<CardVo> cardVoList = new ArrayList<>();
        for (Long id : list) {
            CardVo cardVo = searchById(id);
            if (cardVo != null) { // 如果 searchById 返回了非空的 CardVo 对象，则添加到列表中
                cardVoList.add(cardVo);
            }
        }

        // 返回包含 CardVo 对象的列表
        return cardVoList;

    }
}
