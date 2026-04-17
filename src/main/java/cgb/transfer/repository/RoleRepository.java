package cgb.transfer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import cgb.transfer.entity.Role;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Role findByName(String name);
}
