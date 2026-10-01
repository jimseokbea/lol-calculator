package com.example.demo.repository;

import com.example.demo.entity.ProcessedMatch;
import com.example.demo.entity.ProcessedMatchId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedMatchRepository extends JpaRepository<ProcessedMatch, ProcessedMatchId> {
    boolean existsByUserIdAndMatchId(Long userId, String matchId);
}
