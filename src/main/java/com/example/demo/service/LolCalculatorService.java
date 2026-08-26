package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import com.example.demo.entity.LolRecord;
import com.example.demo.repository.LolRecordRepository;
import org.springframework.stereotype.Service;

    @Service
    public class LolCalculatorService {

        private final LolRecordRepository repository;

        @Value("${riot.api.key}")
        private String apiKey;

        public LolCalculatorService(LolRecordRepository repository) {
            this.repository = repository;
        }

        public int calculate(int minutes) {
            double hours = minutes / 60.0;
            int cost = (int) (hours * 10320);

            LolRecord record = new LolRecord(minutes, cost);

            repository.save(record);

            return (cost);
        }

        public String getRiotPuuid(String gameName, String tagLine) {
            org.springframework.web.client.RestTemplate errandBoy = new org.springframework.web.client.RestTemplate();

            String url = "https://asia.api.riotgames.com/riot/account/v1/accounts/by-riot-id/{gameName}/{tagLine}?api_key={apiKey}";

            String result = errandBoy.getForObject(url, String.class, gameName, tagLine, apiKey);

            int startIndex = result.indexOf("puuid\":\"") + 8;
            int endIndex = result.indexOf("\"", startIndex);
            String puuid = result.substring(startIndex, endIndex);

            return puuid;
        }

        public String getRecentMatchIds(String puuid) {
            org.springframework.web.client.RestTemplate errandBoy = new org.springframework.web.client.RestTemplate();

            String url = "https://asia.api.riotgames.com/lol/match/v5/matches/by-puuid/{puuid}/ids?start=0&count=20&api_key={apiKey}";

            String result = errandBoy.getForObject(url, String.class, puuid, apiKey);

            return result;
        }
    public int getMatchPlayTime(String matchID){
        org.springframework.web.client.RestTemplate errandBoy = new org.springframework.web.client.RestTemplate();

        String url = "https://asia.api.riotgames.com/lol/match/v5/matches/{matchId}?api_key={apiKey}";

        String result = errandBoy.getForObject(url, String.class, matchID, apiKey);

        int startIndex = result.indexOf("\"gameDuration\":") + 15;
        int endIndex = result.indexOf(",", startIndex);
        String durationStr = result.substring(startIndex, endIndex).trim();

        int durationSeconds = Integer.parseInt(durationStr);
        int durationMinutes = durationSeconds / 60;

        return durationMinutes;
    }
    public String calculateTotalRiotCost(String gameName, String tagLine){
            String puuid = getRiotPuuid(gameName, tagLine);

            String matchIdsStr = getRecentMatchIds(puuid);

        String cleanStr = matchIdsStr.replace("[", "").replace("]", "").replace("\"", "");
        String[] matchIds = cleanStr.split(",");

        int totalMinutes = 0;
        for (int i =0; i < 5; i++){
            totalMinutes += getMatchPlayTime(matchIds[i]);
        }
        int totalCost = calculate(totalMinutes);

        return gameName +"님은 최근 5연패 했으며 5게임 동안 날린 시간은 "+ totalMinutes + "분 최저시급으로는 " + totalCost +"원 입니다!";
    }
    }

