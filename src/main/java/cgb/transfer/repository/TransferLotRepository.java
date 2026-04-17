package cgb.transfer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import cgb.transfer.entity.TransferLot;

import java.time.LocalDate;
import java.util.List;

public interface TransferLotRepository extends JpaRepository<TransferLot, Long> {
    List<TransferLot> findByLotIdAndStateIn(Long lotId, List<String> states);
    List<TransferLot> findByCompletionDateBetweenAndStateIn(LocalDate from, LocalDate to, List<String> states);
    List<TransferLot> findByDestAccountAndStateIn(String destAccount, List<String> states);
}
