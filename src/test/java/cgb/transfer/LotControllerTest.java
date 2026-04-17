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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.fasterxml.jackson.databind.ObjectMapper;

import cgb.transfer.dto.LotRequest;
import cgb.transfer.dto.VirementRequest;
import cgb.transfer.entity.Account;
import cgb.transfer.repository.AccountRepository;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "user")
public class LotControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AccountRepository accountRepository;

    private String sourceIban;
    private String destIban;

    @BeforeEach
    void setUp() {
        List<Account> accounts = accountRepository.findAll();
        sourceIban = accounts.get(0).getAccountNumber();
        destIban = accounts.get(5).getAccountNumber();
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
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().findAndRegisterModules().writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.numLot").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.etat").value("received"));
    }

    @Test
    void testGetLotNotFound() throws Exception {
        mockMvc.perform(get("/api/lots/99999"))
                .andExpect(status().isNotFound());
    }
}
