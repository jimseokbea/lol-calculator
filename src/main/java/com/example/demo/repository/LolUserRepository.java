package com.example.demo.repository;

import com.example.demo.entity.LolUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LolUserRepository extends JpaRepository<LolUser, Long> {
    Optional<LolUser> findByGameNameAndTagLine(String gameName, String tagLine);
}
