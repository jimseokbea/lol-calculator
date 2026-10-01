package com.example.demo.service;

import com.example.demo.entity.LolUser;
import com.example.demo.entity.ProcessedMatch;
import com.example.demo.repository.LolUserRepository;
import com.example.demo.repository.ProcessedMatchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class RiotSettlementService {

    private static final int MATCH_PAGE_SIZE = 20;

    private final LolUserRepository lolUserRepository;
    private final ProcessedMatchRepository processedMatchRepository;
    private final LolCalculatorService riotService;
    private final TransactionTemplate userTransaction;

    public RiotSettlementService(
            LolUserRepository lolUserRepository,
            ProcessedMatchRepository processedMatchRepository,
            LolCalculatorService riotService,
            PlatformTransactionManager transactionManager
    ) {
        this.lolUserRepository = lolUserRepository;
        this.processedMatchRepository = processedMatchRepository;
        this.riotService = riotService;
        this.userTransaction = new TransactionTemplate(transactionManager);
        // Isolate each user even when called from a transactional Batch tasklet.
        this.userTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
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

    public int settleAllUsers() {
        List<LolUser> users = lolUserRepository.findAll();
        int settledUserCount = 0;

        for (LolUser user : users) {
            try {
                userTransaction.executeWithoutResult(status -> {
                    LolUser managedUser = lolUserRepository.findById(user.getId()).orElseThrow();
                    settleUser(managedUser);
                });
                settledUserCount++;
            } catch (RuntimeException e) {
                System.err.println("[Riot 정산 실패] userId=" + user.getId() + ", reason=" + e.getMessage());
            }
        }

        return settledUserCount;
    }

    private void settleUser(LolUser user) {
        int addedMinutes = 0;
        int start = 0;
        Set<String> seenMatchIds = new HashSet<>();
        // Scan all available pages: older gaps may exist from previously failed settlements.
        while (true) {
            List<String> matchIds = riotService.getMatchIdPage(user.getPuuid(), start, MATCH_PAGE_SIZE);
            for (String matchId : matchIds) {
                if (!seenMatchIds.add(matchId)
                        || processedMatchRepository.existsByUserIdAndMatchId(user.getId(), matchId)) {
                    continue;
                }
                int playMinutes = riotService.getMatchPlayTime(matchId);
                processedMatchRepository.save(new ProcessedMatch(matchId, user.getId(), playMinutes));
                addedMinutes += playMinutes;
            }
            if (matchIds.size() < MATCH_PAGE_SIZE) {
                break;
            }
            start += matchIds.size();
        }

        if (addedMinutes > 0) {
            user.addPlayTime(addedMinutes);
            lolUserRepository.save(user);
        }
    }
}
