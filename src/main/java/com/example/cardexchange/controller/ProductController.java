package com.example.cardexchange.controller;

import com.example.cardexchange.Service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/product")
public class ProductController {
    @Autowired
    private ProductService productService;

    @GetMapping("/select_and_update")
    public void selectAndAdd(@RequestParam("productId")Integer productId, @RequestParam("oldUserId")Long oldUserId, @RequestParam("newUserId")Long newUserId){
        productService.selectAndAdd(productId,oldUserId,newUserId);
    }
}
