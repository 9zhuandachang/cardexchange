package com.example.cardexchange.Service;
import com.example.cardexchange.Pojo.CardVo;
import com.example.cardexchange.Repository.CardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

public interface CardService {
    List<CardVo> searchByTeam(String team);
}
