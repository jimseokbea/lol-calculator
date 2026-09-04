package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "processed_match")
public class ProcessedMatch {

    @Id
    @Column(name = "match_id", nullable = false, length = 50)
    private String matchId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "play_minutes", nullable = false)
    private int playMinutes;

    protected ProcessedMatch() {
    }

    public ProcessedMatch(String matchId, Long userId, int playMinutes) {
        this.matchId = matchId;
        this.userId = userId;
        this.playMinutes = playMinutes;
    }

    public String getMatchId() {
        return matchId;
    }

    public Long getUserId() {
        return userId;
    }

    public int getPlayMinutes() {
        return playMinutes;
    }
}
