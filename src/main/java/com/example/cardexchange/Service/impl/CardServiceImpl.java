package com.example.cardexchange.Service.impl;

import com.example.cardexchange.Entity.Card;
import com.example.cardexchange.Entity.EsportsCards;
import com.example.cardexchange.Entity.ProductCard;
import com.example.cardexchange.Pojo.CardVo;
import com.example.cardexchange.Repository.CardProductRepository;
import com.example.cardexchange.Repository.CardRepository;
import com.example.cardexchange.Repository.EsportsCardRepository;
import com.example.cardexchange.Repository.ProductCardRepository;
import com.example.cardexchange.Service.CardService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CardServiceImpl implements CardService {
    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private EsportsCardRepository esportsCardRepository;

    @Autowired
    private CardProductRepository cardProductRepository;

    @Autowired
    private ProductCardRepository productCardRepository;

    private CardVo buildCardVo(EsportsCards ec, Card card, ProductCard pc) {
        if (ec == null) {
            return null;
        }
        CardVo cardVo = new CardVo();
        BeanUtils.copyProperties(ec, cardVo);
        if (card != null) {
            cardVo.setImg_url(card.getImg_url());
        }
        if (pc != null) {
            cardVo.setPrice(pc.getMoney());
            cardVo.setSellerId(pc.getUserId());
        } else {
            cardVo.setPrice(BigDecimal.ZERO);
        }
        return cardVo;
    }

    @Override
    public List<Card> searchByTeam(String team) {
        List<EsportsCards> byTeam = esportsCardRepository.findByTeam(team);
        List<Long> cardIds = byTeam.stream().map(EsportsCards::getId).collect(Collectors.toList());
        if (cardIds.isEmpty()) {
            return Collections.emptyList();
        }
        return cardRepository.findAllById(cardIds);
    }

    @Override
    public CardVo searchById(Long id) {
        Optional<EsportsCards> optionalEsportsCards = esportsCardRepository.findById(id);
        if (!optionalEsportsCards.isPresent()) {
            return null;
        }
        Card card = cardRepository.findById(id).orElse(null);
        ProductCard productCard = productCardRepository.findById(id.intValue()).orElse(null);
        return buildCardVo(optionalEsportsCards.get(), card, productCard);
    }

    @Override
    public List<CardVo> searchRecomendtions() {
        List<Long> list = cardProductRepository.findTop4CardIdsByMoney();
        List<CardVo> cardVoList = new ArrayList<>();
        for (Long id : list) {
            if (cardVoList.size() >= 4) {
                break;
            }
            CardVo cardVo = searchById(id);
            if (cardVo != null) {
                cardVoList.add(cardVo);
            }
        }
        return cardVoList;
    }

    @Override
    public List<CardVo> listCards(String keyword, String team, String grade, String sort) {
        List<EsportsCards> allEsports = esportsCardRepository.findAll();
        Map<Long, Card> cardMap = cardRepository.findAll().stream()
                .collect(Collectors.toMap(Card::getId, Function.identity(), (a, b) -> a));
        Map<Long, ProductCard> productMap = productCardRepository.findAll().stream()
                .collect(Collectors.toMap(pc -> (long) pc.getId(), Function.identity(), (a, b) -> a));

        String kw = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        String teamFilter = team == null ? "" : team.trim().toLowerCase(Locale.ROOT);
        String gradeFilter = grade == null ? "" : grade.trim().toUpperCase(Locale.ROOT);

        List<CardVo> result = allEsports.stream()
                .map(ec -> buildCardVo(ec, cardMap.get(ec.getId()), productMap.get(ec.getId())))
                .filter(Objects::nonNull)
                .filter(vo -> {
                    if (!teamFilter.isEmpty() && !"all".equals(teamFilter)) {
                        if (vo.getTeam() == null || !vo.getTeam().equalsIgnoreCase(teamFilter)) {
                            return false;
                        }
                    }
                    if (!gradeFilter.isEmpty() && !"ALL".equals(gradeFilter)) {
                        if (vo.getGrade() == null || !String.valueOf(vo.getGrade()).equalsIgnoreCase(gradeFilter)) {
                            return false;
                        }
                    }
                    if (!kw.isEmpty()) {
                        String haystack = String.join(" ",
                                safe(vo.getTeam()),
                                safe(vo.getPlayerName()),
                                safe(vo.getSeries()),
                                safe(vo.getPublisher()),
                                safe(vo.getCardNumber()),
                                safe(vo.getCardType()),
                                safe(vo.getParallel()),
                                vo.getReleaseYear() == null ? "" : String.valueOf(vo.getReleaseYear())
                        ).toLowerCase(Locale.ROOT);
                        return haystack.contains(kw);
                    }
                    return true;
                })
                .collect(Collectors.toList());

        if ("price_desc".equalsIgnoreCase(sort)) {
            result.sort(Comparator.comparing(CardVo::getPrice, Comparator.nullsLast(Comparator.reverseOrder())));
        } else if ("price_asc".equalsIgnoreCase(sort)) {
            result.sort(Comparator.comparing(CardVo::getPrice, Comparator.nullsLast(Comparator.naturalOrder())));
        } else if ("grade".equalsIgnoreCase(sort)) {
            result.sort(Comparator.comparingInt((CardVo vo) -> gradeRank(vo.getGrade()))
                    .thenComparing(CardVo::getPrice, Comparator.nullsLast(Comparator.reverseOrder())));
        } else {
            result.sort(Comparator.comparing(CardVo::getPrice, Comparator.nullsLast(Comparator.reverseOrder())));
        }

        return result;
    }

    @Override
    public List<CardVo> listUserCards(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return listCards(null, null, null, "price_desc").stream()
                .filter(vo -> userId.equals(vo.getSellerId()))
                .collect(Collectors.toList());
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private int gradeRank(Character grade) {
        if (grade == null) return 99;
        char c = Character.toUpperCase(grade);
        if (c == 'S') return 1;
        if (c == 'A') return 2;
        if (c == 'B') return 3;
        return 10;
    }
}
