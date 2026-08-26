package com.example.demo.scheduler;

import com.example.demo.service.LolCalculatorService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LolScheduler {

    private final LolCalculatorService service;

    public LolScheduler(LolCalculatorService service) {
        this.service = service;
    }


    @Scheduled(cron = "0 0 0 * * *")
    public void autoCalculate() {
        System.out.println("⏰ [스케줄러 작동] 롤 전적 자동 수집을 시작합니다...");

        // 주방장에게 내 닉네임으로 계산하라고 시킵니다.
        String result = service.calculateTotalRiotCost("익두스", "kr1");

        System.out.println("✅ [스케줄러 완료] " + result);
    }
}
