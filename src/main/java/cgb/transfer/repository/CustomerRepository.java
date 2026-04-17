package cgb.transfer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import cgb.transfer.entity.Customer;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Customer findByName(String name);
}
