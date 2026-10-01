package com.example.demo.scheduler;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LolScheduler {

    private final JobOperator jobOperator;
    private final Job riotSettlementJob;

    public LolScheduler(JobOperator jobOperator, Job riotSettlementJob) {
        this.jobOperator = jobOperator;
        this.riotSettlementJob = riotSettlementJob;
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void runDailySettlement() throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters();

        jobOperator.start(riotSettlementJob, jobParameters);
    }
}
