package cgb.transfer.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import cgb.transfer.entity.Account;

public interface AccountRepository extends JpaRepository<Account, String> {
}

