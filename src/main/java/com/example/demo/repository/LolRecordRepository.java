package com.example.demo.repository;

import com.example.demo.entity.LolRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LolRecordRepository extends JpaRepository<LolRecord, Long> {
}
