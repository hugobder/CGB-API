package cgb.transfer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import cgb.transfer.entity.Account;
import cgb.transfer.entity.Transfer;
import cgb.transfer.exception.AccountNotFoundException;
import cgb.transfer.exception.AntidatedTransferException;
import cgb.transfer.exception.InsufficientFundsException;
import cgb.transfer.exception.NegativeAmountException;
import cgb.transfer.repository.AccountRepository;
import cgb.transfer.repository.TransferRepository;
import cgb.transfer.service.TransferService;

@ExtendWith(MockitoExtension.class)
public class TransferServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransferRepository transferRepository;

    @InjectMocks
    private TransferService transferService;

    private Account sourceAccount;
    private Account destAccount;

    @BeforeEach
    void setUp() {
        sourceAccount = new Account();
        sourceAccount.setAccountNumber("FR7630006000011234567890189");
        sourceAccount.setSolde(1000.00);

        destAccount = new Account();
        destAccount.setAccountNumber("FR7630006000019876543210987");
        destAccount.setSolde(500.00);
    }

    @Test
    void testCreateTransfer_Success() throws Exception {
        when(accountRepository.findById("FR7630006000011234567890189")).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findById("FR7630006000019876543210987")).thenReturn(Optional.of(destAccount));
        when(transferRepository.save(any(Transfer.class))).thenAnswer(invocation -> {
            Transfer t = invocation.getArgument(0);
            t.setId(1L);
            return t;
        });

        Transfer result = transferService.createTransfer(
                "FR7630006000011234567890189", "FR7630006000019876543210987",
                200.0, LocalDate.now(), "Test transfer");

        assertNotNull(result);
        assertEquals(200.0, result.getAmount());
        assertEquals(800.0, sourceAccount.getSolde());
        assertEquals(700.0, destAccount.getSolde());
        verify(accountRepository, times(2)).save(any(Account.class));
    }

    @Test
    void testCreateTransfer_NegativeAmount() {
        assertThrows(NegativeAmountException.class, () ->
                transferService.createTransfer("SRC", "DST", -100.0, LocalDate.now(), "test"));
    }

    @Test
    void testCreateTransfer_ZeroAmount() {
        assertThrows(NegativeAmountException.class, () ->
                transferService.createTransfer("SRC", "DST", 0.0, LocalDate.now(), "test"));
    }

    @Test
    void testCreateTransfer_AntidatedTransfer() {
        assertThrows(AntidatedTransferException.class, () ->
                transferService.createTransfer("SRC", "DST", 100.0, LocalDate.of(2020, 1, 1), "test"));
    }

    @Test
    void testCreateTransfer_NullDateUsesToday() throws Exception {
        when(accountRepository.findById("FR7630006000011234567890189")).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findById("FR7630006000019876543210987")).thenReturn(Optional.of(destAccount));
        when(transferRepository.save(any(Transfer.class))).thenAnswer(invocation -> {
            Transfer t = invocation.getArgument(0);
            t.setId(1L);
            return t;
        });

        Transfer result = transferService.createTransfer(
                "FR7630006000011234567890189", "FR7630006000019876543210987",
                100.0, null, "test");

        assertEquals(LocalDate.now(), result.getTransferDate());
    }

    @Test
    void testCreateTransfer_SourceNotFound() {
        when(accountRepository.findById("UNKNOWN")).thenReturn(Optional.empty());
        assertThrows(AccountNotFoundException.class, () ->
                transferService.createTransfer("UNKNOWN", "FR7630006000019876543210987", 100.0, LocalDate.now(), "test"));
    }

    @Test
    void testCreateTransfer_DestinationNotFound() {
        when(accountRepository.findById("FR7630006000011234567890189")).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findById("UNKNOWN")).thenReturn(Optional.empty());
        assertThrows(AccountNotFoundException.class, () ->
                transferService.createTransfer("FR7630006000011234567890189", "UNKNOWN", 100.0, LocalDate.now(), "test"));
    }

    @Test
    void testCreateTransfer_InsufficientFunds() {
        when(accountRepository.findById("FR7630006000011234567890189")).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findById("FR7630006000019876543210987")).thenReturn(Optional.of(destAccount));
        assertThrows(InsufficientFundsException.class, () ->
                transferService.createTransfer("FR7630006000011234567890189", "FR7630006000019876543210987",
                        99999.0, LocalDate.now(), "test"));
    }
}
