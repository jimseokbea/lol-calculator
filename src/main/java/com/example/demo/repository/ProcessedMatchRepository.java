package com.example.demo.repository;

import com.example.demo.entity.ProcessedMatch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedMatchRepository extends JpaRepository<ProcessedMatch, String> {
}
