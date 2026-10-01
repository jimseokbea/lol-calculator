package com.example.demo.service;

import com.example.demo.entity.LolUser;
import com.example.demo.entity.ProcessedMatch;
import com.example.demo.entity.ProcessedMatchId;
import com.example.demo.repository.LolUserRepository;
import com.example.demo.repository.ProcessedMatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(RiotSettlementServiceTest.Config.class)
class RiotSettlementServiceTest {
    @Autowired LolUserRepository users;
    @Autowired ProcessedMatchRepository matches;
    @Autowired LolCalculatorService riot;
    @Autowired RiotSettlementService settlements;
    @Autowired PlatformTransactionManager transactionManager;

    @BeforeEach
    void resetData() {
        matches.deleteAll();
        users.deleteAll();
        reset(riot);
        when(riot.getMatchIdPage(anyString(), anyInt(), eq(20))).thenReturn(List.of());
        when(riot.getMatchPlayTime(anyString())).thenReturn(30);
    }

    @Test
    void rollsBackFailedUserAndRetriesWithoutLosingMinutes() {
        LolUser failed = users.save(new LolUser("failed", "KR1", "failed-puuid"));
        LolUser healthy = users.save(new LolUser("healthy", "KR1", "healthy-puuid"));
        when(riot.getMatchIdPage("failed-puuid", 0, 20)).thenReturn(List.of("A", "B"));
        when(riot.getMatchIdPage("healthy-puuid", 0, 20)).thenReturn(List.of("C"));
        when(riot.getMatchPlayTime("B")).thenThrow(new IllegalStateException("Riot unavailable"));

        // Spring Batch surrounds the tasklet with a transaction: each user must be isolated.
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            assertEquals(1, settlements.settleAllUsers());
            status.setRollbackOnly();
        });
        assertEquals(0, users.findById(failed.getId()).orElseThrow().getTotalPlayMinutes());
        assertFalse(matches.existsByUserIdAndMatchId(failed.getId(), "A"));
        assertEquals(30, users.findById(healthy.getId()).orElseThrow().getTotalPlayMinutes());
        assertTrue(matches.existsByUserIdAndMatchId(healthy.getId(), "C"));

        when(riot.getMatchPlayTime("B")).thenReturn(40);
        assertEquals(2, settlements.settleAllUsers());
        LolUser recovered = users.findById(failed.getId()).orElseThrow();
        assertEquals(70, recovered.getTotalPlayMinutes());
        assertEquals(12040, recovered.getTotalWastedCost());
        assertEquals(3, matches.count());
        settlements.settleAllUsers();
        assertEquals(70, users.findById(failed.getId()).orElseThrow().getTotalPlayMinutes());
        assertEquals(3, matches.count());
    }

    @Test
    void sharedMatchIsCreditedOnceToEachUser() {
        LolUser first = users.save(new LolUser("first", "KR1", "first-puuid"));
        LolUser second = users.save(new LolUser("second", "KR1", "second-puuid"));
        when(riot.getMatchIdPage(anyString(), eq(0), eq(20))).thenReturn(List.of("shared"));
        assertEquals(2, settlements.settleAllUsers());
        assertEquals(2, matches.count());
        assertTrue(matches.existsById(new ProcessedMatchId(first.getId(), "shared")));
        assertTrue(matches.existsById(new ProcessedMatchId(second.getId(), "shared")));
        settlements.settleAllUsers();
        assertEquals(30, users.findById(first.getId()).orElseThrow().getTotalPlayMinutes());
        assertEquals(5160, users.findById(second.getId()).orElseThrow().getTotalWastedCost());
        assertEquals(2, matches.count());
    }

    @Test
    void paginatesPastTwentyAndPastAlreadyProcessedMatches() {
        LolUser user = users.save(new LolUser("many", "KR1", "many-puuid"));
        user.addPlayTime(30);
        users.save(user);
        matches.save(new ProcessedMatch("M0", user.getId(), 30));
        List<String> firstPage = IntStream.range(0, 20).mapToObj(i -> "M" + i).toList();
        when(riot.getMatchIdPage("many-puuid", 0, 20)).thenReturn(firstPage);
        // Duplicate IDs can occur when new games move the offset between requests.
        when(riot.getMatchIdPage("many-puuid", 20, 20)).thenReturn(List.of("M19", "M20", "M21"));
        assertEquals(1, settlements.settleAllUsers());
        assertEquals(22, matches.count());
        assertEquals(660, users.findById(user.getId()).orElseThrow().getTotalPlayMinutes());
        verify(riot, never()).getMatchPlayTime("M0");
        verify(riot, times(1)).getMatchPlayTime("M19");
        settlements.settleAllUsers();
        assertEquals(22, matches.count());
    }

    @Test
    void laterPageFailureRollsBackEarlierPage() {
        LolUser user = users.save(new LolUser("many", "KR1", "many-puuid"));
        when(riot.getMatchIdPage("many-puuid", 0, 20))
                .thenReturn(IntStream.range(0, 20).mapToObj(i -> "M" + i).toList());
        when(riot.getMatchIdPage("many-puuid", 20, 20)).thenThrow(new IllegalStateException("page failure"));
        assertEquals(0, settlements.settleAllUsers());
        assertEquals(0, matches.count());
        assertEquals(0, users.findById(user.getId()).orElseThrow().getTotalWastedCost());
    }

    @Test
    void databaseWriteFailureAlsoRollsBackUserAndMarkers() {
        LolUser user = users.save(new LolUser("db-failure", "KR1", "db-puuid"));
        when(riot.getMatchIdPage("db-puuid", 0, 20)).thenReturn(List.of("valid", "X".repeat(51)));
        assertEquals(0, settlements.settleAllUsers());
        assertEquals(0, matches.count());
        LolUser unchanged = users.findById(user.getId()).orElseThrow();
        assertEquals(0, unchanged.getTotalPlayMinutes());
        assertEquals(0, unchanged.getTotalWastedCost());
    }

    @Configuration
    @EnableJpaRepositories(basePackages = "com.example.demo.repository")
    static class Config {
        @Bean DataSource dataSource() {
            return new EmbeddedDatabaseBuilder().generateUniqueName(true).setType(EmbeddedDatabaseType.H2).build();
        }
        @Bean LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
            var factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(dataSource);
            factory.setPackagesToScan("com.example.demo.entity");
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop"));
            return factory;
        }
        @Bean PlatformTransactionManager transactionManager(EntityManagerFactory factory) {
            return new JpaTransactionManager(factory);
        }
        @Bean LolCalculatorService riotService() { return mock(LolCalculatorService.class); }
        @Bean RiotSettlementService settlementService(LolUserRepository users,
                ProcessedMatchRepository matches, LolCalculatorService riot,
                PlatformTransactionManager transactionManager) {
            return new RiotSettlementService(users, matches, riot, transactionManager);
        }
    }
}
