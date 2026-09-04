package com.example.demo.service;

import com.example.demo.entity.LolRecord;
import com.example.demo.repository.LolRecordRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

@Service
public class LolCalculatorService {

    private static final int HOURLY_WAGE = 10320;

    private final LolRecordRepository repository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${riot.api.key}")
    private String apiKey;

    public LolCalculatorService(LolRecordRepository repository) {
        this.repository = repository;
    }

    public int calculate(int minutes) {
        int cost = calculateCost(minutes);
        repository.save(new LolRecord(minutes, cost));
        return cost;
    }

    public int calculateCost(int minutes) {
        return (int) ((minutes / 60.0) * HOURLY_WAGE);
    }

    public String getRiotPuuid(String gameName, String tagLine) {
        String url = "https://asia.api.riotgames.com/riot/account/v1/accounts/by-riot-id/{gameName}/{tagLine}?api_key={apiKey}";
        String result = restTemplate.getForObject(url, String.class, gameName, tagLine, apiKey);

        if (result == null) {
            throw new IllegalStateException("Riot 계정 조회 결과가 비어 있습니다.");
        }

        int startIndex = result.indexOf("puuid\":\"") + 8;
        int endIndex = result.indexOf("\"", startIndex);
        if (startIndex < 8 || endIndex < 0) {
            throw new IllegalStateException("Riot 계정 응답에서 PUUID를 찾지 못했습니다.");
        }

        return result.substring(startIndex, endIndex);
    }

    public String getRecentMatchIds(String puuid) {
        String url = "https://asia.api.riotgames.com/lol/match/v5/matches/by-puuid/{puuid}/ids?start=0&count=20&api_key={apiKey}";
        String result = restTemplate.getForObject(url, String.class, puuid, apiKey);

        if (result == null) {
            throw new IllegalStateException("최근 경기 조회 결과가 비어 있습니다.");
        }
        return result;
    }

    public List<String> getRecentMatchIdList(String puuid) {
        String rawMatchIds = getRecentMatchIds(puuid);
        String clean = rawMatchIds
                .replace("[", "")
                .replace("]", "")
                .replace("\"", "")
                .trim();

        if (clean.isEmpty()) {
            return List.of();
        }

        return Arrays.stream(clean.split(","))
                .map(String::trim)
                .filter(matchId -> !matchId.isEmpty())
                .toList();
    }

    public int getMatchPlayTime(String matchId) {
        String url = "https://asia.api.riotgames.com/lol/match/v5/matches/{matchId}?api_key={apiKey}";
        String result = restTemplate.getForObject(url, String.class, matchId, apiKey);

        if (result == null) {
            throw new IllegalStateException("경기 상세 조회 결과가 비어 있습니다.");
        }

        int startIndex = result.indexOf("\"gameDuration\":") + 15;
        int endIndex = result.indexOf(",", startIndex);
        if (startIndex < 15 || endIndex < 0) {
            throw new IllegalStateException("경기 응답에서 gameDuration을 찾지 못했습니다.");
        }

        int durationSeconds = Integer.parseInt(result.substring(startIndex, endIndex).trim());
        return durationSeconds / 60;
    }

    public String calculateTotalRiotCost(String gameName, String tagLine) {
        String puuid = getRiotPuuid(gameName, tagLine);
        List<String> matchIds = getRecentMatchIdList(puuid);

        int totalMinutes = matchIds.stream()
                .limit(5)
                .mapToInt(this::getMatchPlayTime)
                .sum();

        int totalCost = calculate(totalMinutes);
        return gameName + "님의 최근 5경기 플레이 시간은 " + totalMinutes
                + "분이며 최저시급 기준 기회비용은 " + totalCost + "원입니다.";
    }
}
