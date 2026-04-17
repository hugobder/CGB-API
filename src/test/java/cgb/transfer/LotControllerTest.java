package cgb.transfer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.fasterxml.jackson.databind.ObjectMapper;

import cgb.transfer.dto.LotRequest;
import cgb.transfer.dto.VirementRequest;
import cgb.transfer.entity.Account;
import cgb.transfer.repository.AccountRepository;

@SpringBootTest
@AutoConfigureMockMvc
public class LotControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AccountRepository accountRepository;

    private String sourceIban;
    private String destIban;
    private String jwtToken;

    @BeforeEach
    void setUp() throws Exception {
        List<Account> accounts = accountRepository.findAll();
        sourceIban = accounts.get(0).getAccountNumber();
        destIban = accounts.get(5).getAccountNumber();

        // Login to get JWT token
        String loginBody = "{\"username\":\"padelphi\",\"password\":\"password123\"}";
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody))
                .andReturn();
        jwtToken = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    void testSubmitLotReturnsReceived() throws Exception {
        VirementRequest vr = new VirementRequest();
        vr.setDestAccount(destIban);
        vr.setAmount(10.0);
        vr.setDescription("Test virement lot");

        LotRequest request = new LotRequest();
        request.setRefLot("2026-04-17-01");
        request.setSourceAccount(sourceIban);
        request.setDescriptionLot("Lot de test");
        request.setVirements(List.of(vr));

        mockMvc.perform(post("/api/lots")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().findAndRegisterModules().writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.numLot").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.etat").value("received"));
    }

    @Test
    void testGetLotNotFound() throws Exception {
        mockMvc.perform(get("/api/lots/99999")
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNotFound());
    }
}
