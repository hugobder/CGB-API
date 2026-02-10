package cgb.transfer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cgb.transfer.entity.Transfer;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, Long> {
}