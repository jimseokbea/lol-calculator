package com.example.demo.batch;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;



@Configuration
@EnableBatchProcessing // 배치 기능 활성화
@EnableJdbcJobRepository // (만약 ChatGPT가 준 코드에 이 어노테이션이 있었다면 주석을 풀고 사용해주세요!)
public class LolBatchConfig {

    @Bean
    public Job testJob(JobRepository jobRepository, Step testStep) {
        return new JobBuilder("testJob", jobRepository)
                .start(testStep)
                .build();
    }

    @Bean
    public Step testStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("testStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("🚀 [스프링 배치] 컨베이어 벨트 작동 테스트 완료!");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    // 🌟 매번 새로운 시간(run.id)으로 배치를 실행해주는 마법의 실행기
    @Bean
    public ApplicationRunner jobRunner(JobOperator jobOperator, Job testJob) {
        return args -> {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("run.id", System.currentTimeMillis())
                    .toJobParameters();
            jobOperator.start(testJob, jobParameters);
        };
    }
}
