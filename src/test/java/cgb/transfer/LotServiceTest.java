package cgb.transfer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import cgb.transfer.dto.LotRequest;
import cgb.transfer.dto.LotResponse;
import cgb.transfer.dto.VirementRequest;
import cgb.transfer.entity.Account;
import cgb.transfer.entity.Lot;
import cgb.transfer.entity.TransferLot;
import cgb.transfer.repository.AccountRepository;
import cgb.transfer.repository.LotRepository;
import cgb.transfer.repository.TransferLotRepository;
import cgb.transfer.service.LotService;

@ExtendWith(MockitoExtension.class)
public class LotServiceTest {

    @Mock private LotRepository lotRepository;
    @Mock private TransferLotRepository transferLotRepository;
    @Mock private AccountRepository accountRepository;
    @InjectMocks private LotService lotService;

    @Test
    void testSubmitLotReturnsReceivedState() {
        VirementRequest vr = new VirementRequest();
        vr.setDestAccount("FR1234567890123456789012345");
        vr.setAmount(100.0);
        vr.setDescription("Test");

        LotRequest request = new LotRequest();
        request.setRefLot("2026-04-17-01");
        request.setSourceAccount("FR9876543210987654321098765");
        request.setDescriptionLot("Test lot");
        request.setVirements(List.of(vr));

        when(lotRepository.save(any(Lot.class))).thenAnswer(i -> {
            Lot l = i.getArgument(0);
            l.setId(1L);
            return l;
        });
        when(lotRepository.findById(1L)).thenReturn(Optional.empty());

        LotResponse response = lotService.submitLot(request);

        assertEquals("received", response.getEtat());
        assertEquals(1L, response.getNumLot());
        assertNotNull(response.getDateLancement());
    }

    @Test
    void testProcessLotSetsSuccessState() {
        Account source = new Account();
        source.setAccountNumber("FR0000000000000000000000001");
        source.setSolde(5000.0);

        Account dest = new Account();
        dest.setAccountNumber("FR0000000000000000000000002");
        dest.setSolde(100.0);

        Lot lot = new Lot();
        lot.setId(1L);
        lot.setSourceAccount(source.getAccountNumber());
        lot.setState("received");

        TransferLot tl = new TransferLot();
        tl.setId(1L);
        tl.setDestAccount(dest.getAccountNumber());
        tl.setAmount(200.0);
        tl.setState("waiting");
        tl.setLot(lot);

        when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));
        when(transferLotRepository.findByLotIdAndStateIn(1L, List.of("waiting"))).thenReturn(List.of(tl));
        when(accountRepository.findById(source.getAccountNumber())).thenReturn(Optional.of(source));
        when(accountRepository.findById(dest.getAccountNumber())).thenReturn(Optional.of(dest));

        lotService.processLot(1L);

        assertEquals("success", tl.getState());
        assertEquals("closed", lot.getState());
        assertEquals(4800.0, source.getSolde());
        assertEquals(300.0, dest.getSolde());
    }

    @Test
    void testProcessLotSetsDelayedWhenInsufficientFunds() {
        Account source = new Account();
        source.setAccountNumber("FR0000000000000000000000001");
        source.setSolde(50.0);

        Lot lot = new Lot();
        lot.setId(1L);
        lot.setSourceAccount(source.getAccountNumber());
        lot.setState("received");

        TransferLot tl = new TransferLot();
        tl.setId(1L);
        tl.setDestAccount("FR0000000000000000000000002");
        tl.setAmount(200.0);
        tl.setState("waiting");
        tl.setLot(lot);

        when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));
        when(transferLotRepository.findByLotIdAndStateIn(1L, List.of("waiting"))).thenReturn(List.of(tl));
        when(accountRepository.findById(source.getAccountNumber())).thenReturn(Optional.of(source));
        when(accountRepository.findById("FR0000000000000000000000002")).thenReturn(Optional.of(new Account()));

        lotService.processLot(1L);

        assertEquals("delayed", tl.getState());
    }

    @Test
    void testProcessLotSetsFailureWhenDestNotFound() {
        Account source = new Account();
        source.setAccountNumber("FR0000000000000000000000001");
        source.setSolde(5000.0);

        Lot lot = new Lot();
        lot.setId(1L);
        lot.setSourceAccount(source.getAccountNumber());
        lot.setState("received");

        TransferLot tl = new TransferLot();
        tl.setId(1L);
        tl.setDestAccount("UNKNOWN");
        tl.setAmount(200.0);
        tl.setState("waiting");
        tl.setLot(lot);

        when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));
        when(transferLotRepository.findByLotIdAndStateIn(1L, List.of("waiting"))).thenReturn(List.of(tl));
        when(accountRepository.findById(source.getAccountNumber())).thenReturn(Optional.of(source));
        when(accountRepository.findById("UNKNOWN")).thenReturn(Optional.empty());

        lotService.processLot(1L);

        assertEquals("failure", tl.getState());
    }
}
