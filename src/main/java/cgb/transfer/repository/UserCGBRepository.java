package cgb.transfer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import cgb.transfer.entity.UserCGB;

import java.util.Optional;

public interface UserCGBRepository extends JpaRepository<UserCGB, Long> {
    Optional<UserCGB> findByUsername(String username);
}
