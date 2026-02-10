package cgb.transfer.entity;

import jakarta.persistence.*;


@Entity
public class Account {
    @Id
    private String accountNumber;
	private Double solde;

    // Getters and Setters obtenus grace à Data
	
	public Double getSolde() {
		return solde;
	}
	
	public void setSolde(Double solde) {
		this.solde = solde;
	}
	
    public String getAccountNumber() {
		return accountNumber;
	}
    
	public void setAccountNumber(String accountNumber) {
		this.accountNumber = accountNumber;
	}
	
}