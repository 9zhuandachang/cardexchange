package com.example.cardexchange.Service;
import com.example.cardexchange.Repository.CardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
@Service
public class CardService {
    @Autowired
    private CardRepository  cardrepository;
}
