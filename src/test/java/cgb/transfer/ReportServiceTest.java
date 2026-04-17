package cgb.transfer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import cgb.transfer.entity.Lot;
import cgb.transfer.entity.TransferLot;
import cgb.transfer.repository.LotRepository;
import cgb.transfer.repository.TransferLotRepository;
import cgb.transfer.service.NotificationService;
import cgb.transfer.service.ReportService;

@ExtendWith(MockitoExtension.class)
public class ReportServiceTest {

    @Mock private LotRepository lotRepository;
    @Mock private TransferLotRepository transferLotRepository;
    @InjectMocks private ReportService reportService;

    @Mock private JavaMailSender mailSender;
    @InjectMocks private NotificationService notificationService;

    @Test
    void testGenerateLotReport() {
        Lot lot = new Lot();
        lot.setId(1L);
        lot.setRefLot("2026-04-17-01");
        lot.setDateLot(LocalDate.now());
        lot.setState("closed");

        TransferLot success = new TransferLot();
        success.setState("success");
        TransferLot failure = new TransferLot();
        failure.setState("failure");
        TransferLot delayed = new TransferLot();
        delayed.setState("delayed");
        lot.setVirements(List.of(success, failure, delayed));

        when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));

        Map<String, Object> report = reportService.generateLotReport(1L);

        assertNotNull(report);
        assertEquals(1L, report.get("success"));
        assertEquals(1L, report.get("failure"));
        assertEquals(1L, report.get("delayed"));
        assertEquals(3, report.get("totalVirements"));
    }

    @Test
    void testGenerateLotReportNotFound() {
        when(lotRepository.findById(999L)).thenReturn(Optional.empty());
        assertNull(reportService.generateLotReport(999L));
    }

    @Test
    void testGetFailedTransfersByLot() {
        TransferLot tl = new TransferLot();
        tl.setState("failure");
        when(transferLotRepository.findByLotIdAndStateIn(eq(1L), anyList())).thenReturn(List.of(tl));

        List<TransferLot> result = reportService.getFailedTransfersByLot(1L);
        assertEquals(1, result.size());
    }

    @Test
    void testGetFailedTransfersByDateRange() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 12, 31);
        when(transferLotRepository.findByCompletionDateBetweenAndStateIn(eq(from), eq(to), anyList())).thenReturn(List.of());

        List<TransferLot> result = reportService.getFailedTransfersByDateRange(from, to);
        assertNotNull(result);
    }

    @Test
    void testSendLotCompletionEmail() {
        Lot lot = new Lot();
        lot.setId(1L);
        lot.setRefLot("2026-04-17-01");
        lot.setDateLot(LocalDate.now());
        TransferLot success = new TransferLot();
        success.setState("success");
        lot.setVirements(List.of(success));

        when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() -> notificationService.sendLotCompletionEmail(1L, "test@gsb.com"));
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}
