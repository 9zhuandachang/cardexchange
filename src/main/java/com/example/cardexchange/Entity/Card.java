package com.example.cardexchange.Entity;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Card {
    @Id
    long id;
    String img_url;

    public String getImg_url() {
        return img_url;
    }




}
