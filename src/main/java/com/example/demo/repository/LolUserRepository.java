package com.example.demo.repository;

import com.example.demo.entity.LolUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LolUserRepository extends JpaRepository<LolUser, Long> {
}
