package cgb.transfer.exemple;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cgb.transfer.entity.Account;
import cgb.transfer.repository.AccountRepository;
import jakarta.annotation.PostConstruct;

@Component
public class DatabaseInitializer {

	/*
	 * 	@Autowired
	 * 	private final AccountRepository accountRepository;
	 * 
	 * Possibilité de faire une injection par l'attribut, mais il est recommander 
	 * de la faire par constructeur comme présenté ci-dessous
	 * 
	*/
    
	private final AccountRepository accountRepository;
	
    @Autowired
    public DatabaseInitializer(AccountRepository accountRepository) {
		//super();
		this.accountRepository = accountRepository;
	}

	@PostConstruct
    public void init() {
        // Vérifiez si la base de données est vide avant d'insérer des données
        if (accountRepository.count() == 0) {
           insertSampleData(accountRepository);
        }
    }

    public static void insertSampleData(AccountRepository accountRepository) {
        double[] soldes = {
            1000.00, 2500.00, 500.00, 3000.00, 1500.00,
            4000.00, 750.00, 2000.00, 1200.00, 5000.00,
            800.00, 3500.00, 600.00, 1800.00, 2200.00,
            900.00, 4500.00, 1100.00, 300.00, 3200.00
        };
        for (int i = 0; i < 20; i++) {
            Account account = new Account();
            account.setAccountNumber(cgb.utils.IbanGenerator.generateValidIban());
            account.setSolde(soldes[i]);
            accountRepository.save(account);
        }
    }
}
