package cgb.transfer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import cgb.transfer.entity.Account;
import cgb.transfer.entity.Customer;
import cgb.transfer.exception.UnauthorizedAccountException;
import cgb.transfer.repository.AccountRepository;
import cgb.transfer.repository.CustomerRepository;
import cgb.transfer.repository.TransferRepository;
import cgb.transfer.service.TransferService;

@ExtendWith(MockitoExtension.class)
public class CustomerAccountValidationTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private TransferRepository transferRepository;
    @Mock
    private CustomerRepository customerRepository;
    @InjectMocks
    private TransferService transferService;

    private Account sourceAccount;
    private Account destAccount;
    private Account unauthorizedAccount;
    private Customer customer;

    @BeforeEach
    void setUp() {
        sourceAccount = new Account();
        sourceAccount.setAccountNumber("FR7630006000011234567890189");
        sourceAccount.setSolde(5000.00);

        destAccount = new Account();
        destAccount.setAccountNumber("FR7630006000019876543210987");
        destAccount.setSolde(500.00);

        unauthorizedAccount = new Account();
        unauthorizedAccount.setAccountNumber("FR7630006000010000000000001");
        unauthorizedAccount.setSolde(1000.00);

        customer = new Customer();
        customer.setId(1L);
        customer.setName("GSB");
        customer.setMyaccounts(List.of(sourceAccount));
        customer.setRecipientAccounts(List.of(destAccount));
    }

    @Test
    void testTransferWithAuthorizedAccounts() throws Exception {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(accountRepository.findById(sourceAccount.getAccountNumber())).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findById(destAccount.getAccountNumber())).thenReturn(Optional.of(destAccount));
        when(transferRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        assertDoesNotThrow(() -> transferService.createTransfer(
                sourceAccount.getAccountNumber(), destAccount.getAccountNumber(),
                100.0, LocalDate.now(), "Authorized transfer", 1L));
    }

    @Test
    void testTransferWithUnauthorizedSource() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(accountRepository.findById(unauthorizedAccount.getAccountNumber())).thenReturn(Optional.of(unauthorizedAccount));
        when(accountRepository.findById(destAccount.getAccountNumber())).thenReturn(Optional.of(destAccount));

        assertThrows(UnauthorizedAccountException.class, () -> transferService.createTransfer(
                unauthorizedAccount.getAccountNumber(), destAccount.getAccountNumber(),
                100.0, LocalDate.now(), "Unauthorized source", 1L));
    }

    @Test
    void testTransferWithUnauthorizedDestination() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(accountRepository.findById(sourceAccount.getAccountNumber())).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findById(unauthorizedAccount.getAccountNumber())).thenReturn(Optional.of(unauthorizedAccount));

        assertThrows(UnauthorizedAccountException.class, () -> transferService.createTransfer(
                sourceAccount.getAccountNumber(), unauthorizedAccount.getAccountNumber(),
                100.0, LocalDate.now(), "Unauthorized dest", 1L));
    }

    @Test
    void testTransferWithoutCustomerIdSkipsValidation() throws Exception {
        when(accountRepository.findById(sourceAccount.getAccountNumber())).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findById(destAccount.getAccountNumber())).thenReturn(Optional.of(destAccount));
        when(transferRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        assertDoesNotThrow(() -> transferService.createTransfer(
                sourceAccount.getAccountNumber(), destAccount.getAccountNumber(),
                100.0, LocalDate.now(), "No customer check"));
    }
}
