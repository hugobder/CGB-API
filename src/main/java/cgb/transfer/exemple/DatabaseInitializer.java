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

    public static void insertSampleDataOld(AccountRepository accountRepository) {
        // Insérer des comptes d'exemple
        Account account1 = new Account();
        account1.setAccountNumber("123456789");
        account1.setSolde(300.00);
        accountRepository.save(account1);

        Account account2 = new Account();
        account2.setAccountNumber("987654321");
        account2.setSolde(500.00);
        accountRepository.save(account2);

        Account account3 = new Account();
        account3.setAccountNumber("456789123");
        account3.setSolde(2000.00);
        accountRepository.save(account3);
    }
    
    public static void insertSampleData(AccountRepository accountRepository) {
        // Insérer des comptes d'exemple
        Account account1 = new Account();
        account1.setAccountNumber("123456789");
        account1.setSolde(300.00);
        accountRepository.save(account1);

        Account account2 = new Account();
        account2.setAccountNumber("234567891");
        account2.setSolde(500.00);
        accountRepository.save(account2);

        Account account3 = new Account();
        account3.setAccountNumber("345678912");
        account3.setSolde(2000.00);
        accountRepository.save(account3);
        
        Account account4 = new Account();
        account4.setAccountNumber("456789123");
        account4.setSolde(1000.00);
        accountRepository.save(account4);
        
        Account account5 = new Account();
        account5.setAccountNumber("567891234");
        account5.setSolde(1000.00);
        accountRepository.save(account5);
        
        Account account6 = new Account();
        account6.setAccountNumber("678912345");
        account6.setSolde(1000.00);
        accountRepository.save(account6);
        
        Account account7 = new Account();
        account7.setAccountNumber("789123456");
        account7.setSolde(1000.00);
        accountRepository.save(account7);
        
        Account account8 = new Account();
        account8.setAccountNumber("891234568");
        account8.setSolde(1000.00);
        accountRepository.save(account8);
    }
}
