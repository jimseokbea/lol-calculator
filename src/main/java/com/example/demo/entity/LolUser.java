package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "lol_user",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_lol_user_game_tag",
                columnNames = {"game_name", "tag_line"}
        )
)
public class LolUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "game_name", nullable = false)
    private String gameName;

    @Column(name = "tag_line", nullable = false)
    private String tagLine;

    @Column(nullable = false, length = 100)
    private String puuid;

    @Column(name = "total_play_minutes", nullable = false)
    private int totalPlayMinutes;

    @Column(name = "total_wasted_cost", nullable = false)
    private int totalWastedCost;

    protected LolUser() {
    }

    public LolUser(String gameName, String tagLine, String puuid) {
        this.gameName = gameName;
        this.tagLine = tagLine;
        this.puuid = puuid;
    }

    public void addPlayTime(int minutes) {
        this.totalPlayMinutes += minutes;
        this.totalWastedCost += (int) ((minutes / 60.0) * 10320);
    }

    public void updatePuuid(String puuid) {
        this.puuid = puuid;
    }

    public Long getId() {
        return id;
    }

    public String getGameName() {
        return gameName;
    }

    public String getTagLine() {
        return tagLine;
    }

    public String getPuuid() {
        return puuid;
    }

    public int getTotalPlayMinutes() {
        return totalPlayMinutes;
    }

    public int getTotalWastedCost() {
        return totalWastedCost;
    }
}
