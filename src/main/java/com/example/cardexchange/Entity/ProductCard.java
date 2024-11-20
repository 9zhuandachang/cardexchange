package com.example.cardexchange.Entity;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.*;
import java.math.BigDecimal;

@Entity(name = "product_card")
@Table(name = "product_card")
public class ProductCard {

    @Id
    @Column(name = "id")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    @Column(name = "money", precision = 10, scale = 2)
    private BigDecimal money;

    // 省略其他字段和方法，如构造函数、getter和setter等

    // 构造函数
    public ProductCard() {
    }

    // Getter 和 Setter 方法
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public BigDecimal getMoney() {
        return money;
    }

    public void setMoney(BigDecimal money) {
        this.money = money;
    }

    // 省略其他getter和setter方法
}
