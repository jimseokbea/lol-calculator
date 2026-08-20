package com.example.demo.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class LolRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int playMinutes;
    private int wastedCost;

    public LolRecord(){
    }
    public LolRecord(int playMinutes, int wastedCost){
        this.playMinutes= playMinutes;
        this.wastedCost = wastedCost;
    }
}
