package cgb.transfer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import cgb.transfer.dto.LotRequest;
import cgb.transfer.entity.Lot;
import cgb.transfer.entity.TransferLot;
import cgb.transfer.repository.AccountRepository;
import cgb.transfer.repository.LotRepository;
import cgb.transfer.repository.TransferLotRepository;
import cgb.transfer.service.LotService;

@ExtendWith(MockitoExtension.class)
public class ReplayTest {

    @Mock private LotRepository lotRepository;
    @Mock private TransferLotRepository transferLotRepository;
    @Mock private AccountRepository accountRepository;
    @InjectMocks private LotService lotService;

    private Lot createTestLot() {
        Lot lot = new Lot();
        lot.setId(1L);
        lot.setRefLot("2026-04-17-01");
        lot.setSourceAccount("FR0000000000000000000000001");
        lot.setDescriptionLot("Test lot");
        lot.setDateLot(LocalDate.now());
        lot.setState("closed");
        return lot;
    }

    @Test
    void testReplayFromDelayed() {
        Lot lot = createTestLot();
        TransferLot delayed = new TransferLot();
        delayed.setId(1L);
        delayed.setDestAccount("FR0000000000000000000000002");
        delayed.setAmount(100.0);
        delayed.setDescription("Virement test");
        delayed.setState("delayed");
        delayed.setLot(lot);

        when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));
        when(transferLotRepository.findByLotIdAndStateIn(1L, List.of("delayed"))).thenReturn(List.of(delayed));

        LotRequest replay = lotService.replayFromDelayed(1L);

        assertNotNull(replay);
        assertTrue(replay.getDescriptionLot().startsWith("REJEU"));
        assertTrue(replay.getRefLot().contains("REJEU"));
        assertEquals(1, replay.getVirements().size());
        assertTrue(replay.getVirements().get(0).getDescription().startsWith("REJEU"));
    }

    @Test
    void testReplayFromDelayedNoDelayed() {
        Lot lot = createTestLot();
        when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));
        when(transferLotRepository.findByLotIdAndStateIn(1L, List.of("delayed"))).thenReturn(List.of());

        assertNull(lotService.replayFromDelayed(1L));
    }

    @Test
    void testReplayFromIdsExcludesSuccess() {
        Lot lot = createTestLot();

        TransferLot success = new TransferLot();
        success.setId(1L);
        success.setState("success");
        success.setLot(lot);

        TransferLot delayed = new TransferLot();
        delayed.setId(2L);
        delayed.setDestAccount("FR0000000000000000000000003");
        delayed.setAmount(200.0);
        delayed.setDescription("Delayed virement");
        delayed.setState("delayed");
        delayed.setLot(lot);

        when(transferLotRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(success, delayed));

        LotRequest replay = lotService.replayFromIds(List.of(1L, 2L));

        assertNotNull(replay);
        assertEquals(1, replay.getVirements().size());
        assertEquals(200.0, replay.getVirements().get(0).getAmount());
    }

    @Test
    void testReplayFromIdsAllSuccess() {
        TransferLot success = new TransferLot();
        success.setId(1L);
        success.setState("success");

        when(transferLotRepository.findAllById(List.of(1L))).thenReturn(List.of(success));

        assertNull(lotService.replayFromIds(List.of(1L)));
    }
}
