package com.example.cardexchange.controller;

import com.example.cardexchange.Entity.Card;
import com.example.cardexchange.Pojo.CardVo;
import com.example.cardexchange.Service.CardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/card")
public class CardController {
    @Autowired
    private CardService cardService;

    @GetMapping("/search_by_team")
    public List<Card> searchByTeam(@RequestParam("team") String team) {
        return cardService.searchByTeam(team);
    }


    @GetMapping("/search_by_id")
    public CardVo searchByTeamForInfo(@RequestParam("id") Long id) {
        return cardService.searchById(id);
    }

    @GetMapping("/recomendtions")
    public List<CardVo> searchRecomendtions() { return cardService.searchRecomendtions();}

    @GetMapping("/list")
    public List<CardVo> listCards(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "team", required = false) String team,
            @RequestParam(value = "grade", required = false) String grade,
            @RequestParam(value = "sort", required = false) String sort) {
        return cardService.listCards(keyword, team, grade, sort);
    }

    @GetMapping("/user/{userId}")
    public List<CardVo> listUserCards(@PathVariable("userId") Long userId) {
        return cardService.listUserCards(userId);
    }
}
