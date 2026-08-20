package com.example.demo.controller;

import com.example.demo.service.LolCalculatorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LolCalculatorController {

    public final LolCalculatorService service;

    public LolCalculatorController(LolCalculatorService service){
        this.service = service;
    }

    @GetMapping("/calculate")
    public String calculateCost(int minutes){
        int cost = service.calculate(minutes);
        return "당신이 오늘날린 금액은 " + cost + "원입니다.";
    }

    @GetMapping("/riot")
    public String testRiotApi(String gameName, String tagLine){
        return service.getRiotPuuid(gameName, tagLine);
    }
    @GetMapping("/matches")
    public String testMatchIds(String puuid) {
        return service.getRecentMatchIds(puuid);
    }
    @GetMapping("/match-time")
    public String testMachTime(String matchId){
        int minutes = service.getMatchPlayTime(matchId);
        return "이 게임은 " + minutes + "분 동안 플레이 했습니다.";
    }
    @GetMapping("/riot-calculate")
    public String riotCalculate(String gameName, String tagLine) {
        return service.calculateTotalRiotCost(gameName, tagLine);
    }
}
