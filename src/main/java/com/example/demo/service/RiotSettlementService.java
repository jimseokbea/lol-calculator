package com.example.demo.service;

import com.example.demo.entity.LolUser;
import com.example.demo.entity.ProcessedMatch;
import com.example.demo.repository.LolUserRepository;
import com.example.demo.repository.ProcessedMatchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RiotSettlementService {

    private final LolUserRepository lolUserRepository;
    private final ProcessedMatchRepository processedMatchRepository;
    private final LolCalculatorService riotService;

    public RiotSettlementService(
            LolUserRepository lolUserRepository,
            ProcessedMatchRepository processedMatchRepository,
            LolCalculatorService riotService
    ) {
        this.lolUserRepository = lolUserRepository;
        this.processedMatchRepository = processedMatchRepository;
        this.riotService = riotService;
    }

    @Transactional
    public LolUser registerUser(String gameName, String tagLine) {
        String puuid = riotService.getRiotPuuid(gameName, tagLine);

        return lolUserRepository.findByGameNameAndTagLine(gameName, tagLine)
                .map(user -> {
                    user.updatePuuid(puuid);
                    return user;
                })
                .orElseGet(() -> lolUserRepository.save(new LolUser(gameName, tagLine, puuid)));
    }

    @Transactional
    public int settleAllUsers() {
        List<LolUser> users = lolUserRepository.findAll();
        int settledUserCount = 0;

        for (LolUser user : users) {
            try {
                settleUser(user);
                settledUserCount++;
            } catch (RuntimeException e) {
                System.err.println("[Riot 정산 실패] userId=" + user.getId() + ", reason=" + e.getMessage());
            }
        }

        return settledUserCount;
    }

    private void settleUser(LolUser user) {
        List<String> matchIds = riotService.getRecentMatchIdList(user.getPuuid());

        int addedMinutes = 0;
        for (String matchId : matchIds) {
            if (processedMatchRepository.existsById(matchId)) {
                continue;
            }

            int playMinutes = riotService.getMatchPlayTime(matchId);
            processedMatchRepository.save(new ProcessedMatch(matchId, user.getId(), playMinutes));
            addedMinutes += playMinutes;
        }

        if (addedMinutes > 0) {
            user.addPlayTime(addedMinutes);
            lolUserRepository.save(user);
        }
    }
}
