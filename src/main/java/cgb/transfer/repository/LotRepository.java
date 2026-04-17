package cgb.transfer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import cgb.transfer.entity.Lot;

public interface LotRepository extends JpaRepository<Lot, Long> {
}
