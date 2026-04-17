package cgb.transfer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cgb.transfer.entity.Account;
import cgb.transfer.entity.Transfer;
import cgb.transfer.exception.*;
import cgb.transfer.exception.AccountNotFoundException;
import cgb.transfer.exception.AntidatedTransferException;
import cgb.transfer.exception.DeleteTransferException.FailureTransfert;
import cgb.transfer.exception.InsufficientFundsException;
import cgb.transfer.exception.NegativeAmountException;
import cgb.transfer.repository.AccountRepository;
import cgb.transfer.repository.TransferRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.Optional;

@Service
public class TransferService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransferRepository transferRepository;

    
    /*
     * Rappel du cours sur les transactions... Tout ou rien
     */
    @Transactional
    public Transfer createTransfer(String sourceAccountNumber, String destinationAccountNumber,
                                   Double amount, LocalDate transferDate, String description)
            throws AccountNotFoundException, NegativeAmountException, AntidatedTransferException, InsufficientFundsException {

        if (amount == null || amount <= 0) {
            throw new NegativeAmountException("Le montant doit etre strictement positif: " + amount);
        }

        if (transferDate == null) {
            transferDate = LocalDate.now();
        } else if (transferDate.isBefore(LocalDate.now())) {
            throw new AntidatedTransferException("Le virement ne peut pas etre antidate: " + transferDate);
        }

        Account sourceAccount = accountRepository.findById(sourceAccountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Compte source introuvable: " + sourceAccountNumber));
        Account destinationAccount = accountRepository.findById(destinationAccountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Compte destinataire introuvable: " + destinationAccountNumber));

        if (sourceAccount.getSolde().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Fonds insuffisants sur le compte: " + sourceAccountNumber);
        }

        sourceAccount.setSolde(sourceAccount.getSolde() - amount);
        destinationAccount.setSolde(destinationAccount.getSolde() + amount);

        accountRepository.save(sourceAccount);
        accountRepository.save(destinationAccount);

        Transfer transfer = new Transfer();
        transfer.setSourceAccountNumber(sourceAccountNumber);
        transfer.setDestinationAccountNumber(destinationAccountNumber);
        transfer.setAmount(amount);
        transfer.setTransferDate(transferDate);
        transfer.setDescription(description);

        return transferRepository.save(transfer);
    }
    
    
    @Transactional
    public Transfer deleteTransfer(Long id) throws DeleteTransferException {
    	Optional<Transfer> otranfer=transferRepository.findById(id);
    	transferRepository.deleteById(id);
    	if (otranfer.isEmpty())throw new DeleteTransferException(FailureTransfert.OBJECT_NOT_FOUND); 
    	return otranfer.orElse(null);
    }
}


