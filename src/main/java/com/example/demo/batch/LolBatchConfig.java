package com.example.demo.batch;

import com.example.demo.service.RiotSettlementService;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableBatchProcessing
@EnableJdbcJobRepository
public class LolBatchConfig {

    @Bean
    public Job riotSettlementJob(JobRepository jobRepository, Step riotSettlementStep) {
        return new JobBuilder("riotSettlementJob", jobRepository)
                .start(riotSettlementStep)
                .build();
    }

    @Bean
    public Step riotSettlementStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            RiotSettlementService settlementService
    ) {
        return new StepBuilder("riotSettlementStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    int settledUsers = settlementService.settleAllUsers();
                    System.out.println("[Spring Batch] Riot 일일 정산 완료 - 처리 사용자 수: " + settledUsers);
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
