package com.example.cardexchange.config;

import com.example.cardexchange.Entity.Card;
import com.example.cardexchange.Entity.EsportsCards;
import com.example.cardexchange.Entity.ProductCard;
import com.example.cardexchange.Entity.User;
import com.example.cardexchange.Repository.CardRepository;
import com.example.cardexchange.Repository.EsportsCardRepository;
import com.example.cardexchange.Repository.ProductCardRepository;
import com.example.cardexchange.Repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Profile("local")
public class LocalDataInitializer implements CommandLineRunner {

    private final CardRepository cardRepository;
    private final EsportsCardRepository esportsCardRepository;
    private final ProductCardRepository productCardRepository;
    private final UserRepository userRepository;

    public LocalDataInitializer(CardRepository cardRepository,
                                EsportsCardRepository esportsCardRepository,
                                ProductCardRepository productCardRepository,
                                UserRepository userRepository) {
        this.cardRepository = cardRepository;
        this.esportsCardRepository = esportsCardRepository;
        this.productCardRepository = productCardRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            User demoUser = new User();
            demoUser.setUsername("demo");
            demoUser.setPassword("123456");
            demoUser.setEmail("demo@example.com");
            userRepository.save(demoUser);
        }

        if (esportsCardRepository.count() == 0 && cardRepository.count() == 0) {
            String[][] seedCards = {
                {"AL-ALE", "AL", "ALE", "LPL-2024-001", "A", "188.00"},
                {"BLG-BIN", "BLG", "BIN", "LPL-2024-002", "S", "520.00"},
                {"BLG-ELK", "BLG", "ELK", "LPL-2024-003", "S", "460.00"},
                {"BLG-KNIGHT", "BLG", "KNIGHT", "LPL-2024-004", "S", "580.00"},
                {"BLG-LVMAO", "BLG", "LVMAO", "LPL-2024-005", "A", "160.00"},
                {"BLG-ON", "BLG", "ON", "LPL-2024-006", "A", "280.00"},
                {"BLG-WEI", "BLG", "WEI", "LPL-2024-007", "A", "310.00"},
                {"EDG-CRYIN", "EDG", "CRYIN", "LPL-2024-008", "B", "150.00"},
                {"EDG-JIEJIE", "EDG", "JIEJIE", "LPL-2024-009", "S", "420.00"},
                {"IG-YSKM", "IG", "YSKM", "LPL-2024-010", "B", "140.00"},
                {"JDG-RULER", "JDG", "RULER", "LPL-2024-011", "S", "490.00"},
                {"LNG-ZIKA", "LNG", "ZIKA", "LPL-2024-012", "A", "230.00"},
                {"TES-JACKEYLOVE", "TES", "JACKEYLOVE", "LPL-2024-013", "S", "550.00"},
                {"TES-MEIKO", "TES", "MEIKO", "LPL-2024-014", "S", "440.00"},
                {"WBG-CRISP1", "WBG", "CRISP", "LPL-2024-015", "A", "320.00"},
                {"WBG-TARZAN", "WBG", "TARZAN", "LPL-2024-016", "A", "350.00"}
            };

            for (String[] item : seedCards) {
                EsportsCards ec = new EsportsCards();
                ec.setSportsType("Esports");
                ec.setPublisher("LPL Official");
                ec.setSeries("2024 Summer Split");
                ec.setReleaseYear(2024);
                ec.setTeam(item[1]);
                ec.setPlayerName(item[2]);
                ec.setCardNumber(item[3]);
                ec.setGrade(item[4].charAt(0));
                ec.setCardType("Base");
                ec.setParallel("Holo");
                ec.setLimited("S".equals(item[4]));
                ec.setExquisite("S".equals(item[4]));
                ec.setAutograph(false);
                ec.setReviewed(true);
                EsportsCards saved = esportsCardRepository.save(ec);

                Card card = new Card();
                card.setId(saved.getId());
                card.setImg_url(item[0]);
                cardRepository.save(card);

                ProductCard productCard = new ProductCard();
                productCard.setUserId(1L);
                productCard.setMoney(new BigDecimal(item[5]));
                productCardRepository.save(productCard);
            }
        }
    }
}
