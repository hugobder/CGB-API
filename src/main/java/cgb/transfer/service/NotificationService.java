package cgb.transfer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import cgb.transfer.entity.Lot;
import cgb.transfer.entity.TransferLot;
import cgb.transfer.repository.LotRepository;

import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private LotRepository lotRepository;

    public void sendLotCompletionEmail(Long lotId, String recipientEmail) {
        Lot lot = lotRepository.findById(lotId).orElse(null);
        if (lot == null) return;

        List<TransferLot> virements = lot.getVirements();
        long successCount = virements.stream().filter(v -> "success".equals(v.getState())).count();
        long failureCount = virements.size() - successCount;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(recipientEmail);
        message.setSubject("CGB - Traitement du lot " + lot.getRefLot() + " termine");
        message.setText(String.format(
                "Lot: %s\nDate: %s\nTransactions reussies: %d\nTransactions en echec: %d",
                lot.getRefLot(), lot.getDateLot(), successCount, failureCount));
        message.setFrom("noreply@cgb.com");

        mailSender.send(message);
    }
}
