package cgb.transfer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.fasterxml.jackson.databind.ObjectMapper;

import cgb.transfer.entity.Account;
import cgb.transfer.entity.Transfer;
import cgb.transfer.repository.AccountRepository;

@SpringBootTest
@AutoConfigureMockMvc
public class TransferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    private String sourceIban;
    private String destIban;
    private String jwtToken;

    @BeforeEach
    void setUp() throws Exception {
        List<Account> accounts = accountRepository.findAll();
        sourceIban = accounts.get(0).getAccountNumber();
        destIban = accounts.get(1).getAccountNumber();

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
    public void createTransferTest_Success() throws Exception {
        Transfer transfer = new Transfer();
        transfer.setAmount(10.0);
        transfer.setDescription("Test du transfer");
        transfer.setDestinationAccountNumber(destIban);
        transfer.setSourceAccountNumber(sourceIban);
        transfer.setTransferDate(LocalDate.now());
        mockMvc.perform(post("/api/transfers")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(transfer)))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").exists());
    }

    @Test
    public void createTransferTest_InsufficientFunds() throws Exception {
        Transfer transfer = new Transfer();
        transfer.setAmount(999999.0);
        transfer.setDescription("Test insufficient funds");
        transfer.setDestinationAccountNumber(destIban);
        transfer.setSourceAccountNumber(sourceIban);
        transfer.setTransferDate(LocalDate.now());
        mockMvc.perform(post("/api/transfers")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(transfer)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void createTransferTest_NegativeAmount() throws Exception {
        Transfer transfer = new Transfer();
        transfer.setAmount(-50.0);
        transfer.setDescription("Test negative amount");
        transfer.setDestinationAccountNumber(destIban);
        transfer.setSourceAccountNumber(sourceIban);
        transfer.setTransferDate(LocalDate.now());
        mockMvc.perform(post("/api/transfers")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(transfer)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void createTransferTest_AntidatedTransfer() throws Exception {
        Transfer transfer = new Transfer();
        transfer.setAmount(10.0);
        transfer.setDescription("Test antidated");
        transfer.setDestinationAccountNumber(destIban);
        transfer.setSourceAccountNumber(sourceIban);
        transfer.setTransferDate(LocalDate.of(2020, 1, 1));
        mockMvc.perform(post("/api/transfers")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(transfer)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void createTransferTest_AccountNotFound() throws Exception {
        Transfer transfer = new Transfer();
        transfer.setAmount(10.0);
        transfer.setDescription("Test account not found");
        transfer.setDestinationAccountNumber("FR0000000000000000000000000");
        transfer.setSourceAccountNumber(sourceIban);
        transfer.setTransferDate(LocalDate.now());
        mockMvc.perform(post("/api/transfers")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(transfer)))
                .andExpect(status().isNotFound());
    }

    public static String asJsonString(final Object obj) {
        try {
            return new ObjectMapper().findAndRegisterModules().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
