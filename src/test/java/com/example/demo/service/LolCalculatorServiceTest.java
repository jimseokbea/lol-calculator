package com.example.demo.service;

import com.example.demo.repository.LolRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class LolCalculatorServiceTest {
    private LolCalculatorService service;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        service = new LolCalculatorService(mock(LolRecordRepository.class));
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        RestTemplate client = (RestTemplate) ReflectionTestUtils.getField(service, "restTemplate");
        server = MockRestServiceServer.bindTo(client).build();
    }

    @Test
    void sendsPageOffsetAndCountAndParsesIds() {
        server.expect(requestTo("https://asia.api.riotgames.com/lol/match/v5/matches/by-puuid/puuid/ids?start=20&count=20&api_key=test-key"))
                .andRespond(withSuccess("[\"KR_20\", \"KR_21\"]", MediaType.APPLICATION_JSON));
        assertEquals(List.of("KR_20", "KR_21"), service.getMatchIdPage("puuid", 20, 20));
        server.verify();
    }

    @Test
    void acceptsEmptyFinalPage() {
        server.expect(requestTo("https://asia.api.riotgames.com/lol/match/v5/matches/by-puuid/puuid/ids?start=40&count=20&api_key=test-key"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        assertTrue(service.getMatchIdPage("puuid", 40, 20).isEmpty());
        server.verify();
    }

    @Test
    void propagatesRiotFailureToSettlementTransaction() {
        server.expect(requestTo("https://asia.api.riotgames.com/lol/match/v5/matches/by-puuid/puuid/ids?start=20&count=20&api_key=test-key"))
                .andRespond(withServerError());
        assertThrows(RuntimeException.class, () -> service.getMatchIdPage("puuid", 20, 20));
        server.verify();
    }

    @Test
    void rejectsInvalidPages() {
        assertThrows(IllegalArgumentException.class, () -> service.getMatchIdPage("puuid", -1, 20));
        assertThrows(IllegalArgumentException.class, () -> service.getMatchIdPage("puuid", 0, 0));
        assertThrows(IllegalArgumentException.class, () -> service.getMatchIdPage("puuid", 0, 101));
    }
}
