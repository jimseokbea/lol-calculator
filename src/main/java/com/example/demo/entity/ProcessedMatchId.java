package com.example.demo.entity;

import java.io.Serializable;
import java.util.Objects;

public class ProcessedMatchId implements Serializable {
    private Long userId;
    private String matchId;

    public ProcessedMatchId() {
    }

    public ProcessedMatchId(Long userId, String matchId) {
        this.userId = userId;
        this.matchId = matchId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ProcessedMatchId id)) return false;
        return Objects.equals(userId, id.userId) && Objects.equals(matchId, id.matchId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, matchId);
    }
}
