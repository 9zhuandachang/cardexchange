package com.example.cardexchange.Service.impl;

import com.example.cardexchange.Entity.ProductCard;
import com.example.cardexchange.Repository.ProductCardRepository;
import com.example.cardexchange.Service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductCardRepository productCardRepository;
    @Override
    public void selectAndAdd(Integer id, Long oldUserId, Long newUserId) {
        List<ProductCard> byIdAndUserId = productCardRepository.findByIdAndUserId(id, oldUserId);

        for (ProductCard productCard : byIdAndUserId) {
            productCardRepository.updateProductCard(productCard.getId(),productCard.getUserId(),newUserId);
        }
    }
}
