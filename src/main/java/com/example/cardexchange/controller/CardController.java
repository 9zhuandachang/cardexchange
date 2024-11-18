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
    public List<CardVo> searchByTeam(@RequestParam("team") String team) {
        return cardService.searchByTeam(team);
    }
}
