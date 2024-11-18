package com.example.cardexchange.Entity;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Card {
    @Id
    long id;
    String img_url;

    public long getId() {
        return id;
    }

    public String getImg_url() {
        return img_url;
    }

    public void setImg_url(String img_url) {
        this.img_url = img_url;
    }




}
