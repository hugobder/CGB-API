package cgb.transfer.exemple;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cgb.transfer.entity.Account;
import cgb.transfer.entity.Customer;
import cgb.transfer.entity.Role;
import cgb.transfer.entity.UserCGB;
import cgb.transfer.repository.AccountRepository;
import cgb.transfer.repository.CustomerRepository;
import cgb.transfer.repository.RoleRepository;
import cgb.transfer.repository.UserCGBRepository;
import cgb.utils.IbanGenerator;
import jakarta.annotation.PostConstruct;

import java.util.List;

@Component
public class DatabaseInitializer {

    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final UserCGBRepository userCGBRepository;
    private final CustomerRepository customerRepository;

    @Autowired
    public DatabaseInitializer(AccountRepository accountRepository,
                               RoleRepository roleRepository,
                               UserCGBRepository userCGBRepository,
                               CustomerRepository customerRepository) {
        this.accountRepository = accountRepository;
        this.roleRepository = roleRepository;
        this.userCGBRepository = userCGBRepository;
        this.customerRepository = customerRepository;
    }

    @PostConstruct
    public void init() {
        if (accountRepository.count() == 0) {
            insertSampleData();
        }
    }

    private void insertSampleData() {
        // Create 20 accounts with valid IBANs
        double[] soldes = {
            1000.00, 2500.00, 500.00, 3000.00, 1500.00,
            4000.00, 750.00, 2000.00, 1200.00, 5000.00,
            800.00, 3500.00, 600.00, 1800.00, 2200.00,
            900.00, 4500.00, 1100.00, 300.00, 3200.00
        };
        for (int i = 0; i < 20; i++) {
            Account account = new Account();
            account.setAccountNumber(IbanGenerator.generateValidIban());
            account.setSolde(soldes[i]);
            accountRepository.save(account);
        }

        List<Account> allAccounts = accountRepository.findAll();

        // Create roles
        Role comptable = new Role("COMPTABLE");
        Role utilisateur = new Role("UTILISATEUR");
        roleRepository.save(comptable);
        roleRepository.save(utilisateur);

        // Create customer GSB
        Customer gsb = new Customer();
        gsb.setName("Galaxy Swiss Bourdin");
        gsb.setAddress("15 Rue de la Paix, 75002 Paris");
        gsb.setLei("529900T8BM49AURSDO55");
        // First 5 accounts are source accounts for GSB
        gsb.setMyaccounts(allAccounts.subList(0, 5));
        // Accounts 5-19 are recipient/beneficiary accounts for GSB
        gsb.setRecipientAccounts(allAccounts.subList(5, 20));
        customerRepository.save(gsb);

        // Create users
        UserCGB phil = new UserCGB();
        phil.setUsername("padelphi");
        phil.setPassword("password123");
        phil.setEmail("phil.adelphi@gsb.com");
        phil.setRole(comptable);
        phil.setBelongTo(gsb);
        userCGBRepository.save(phil);

        UserCGB pat = new UserCGB();
        pat.setUsername("patchaude");
        pat.setPassword("password123");
        pat.setEmail("pat.atchaude@gsb.com");
        pat.setRole(utilisateur);
        pat.setBelongTo(gsb);
        userCGBRepository.save(pat);
    }
}
