package cgb.transfer.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String address;
    @Column(length = 20)
    private String lei;

    @ManyToMany
    @JoinTable(name = "customer_accounts",
            joinColumns = @JoinColumn(name = "customer_id"),
            inverseJoinColumns = @JoinColumn(name = "account_number"))
    private List<Account> myaccounts = new ArrayList<>();

    @ManyToMany
    @JoinTable(name = "customer_recipient_accounts",
            joinColumns = @JoinColumn(name = "customer_id"),
            inverseJoinColumns = @JoinColumn(name = "account_number"))
    private List<Account> recipientAccounts = new ArrayList<>();

    public Customer() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getLei() { return lei; }
    public void setLei(String lei) { this.lei = lei; }
    public List<Account> getMyaccounts() { return myaccounts; }
    public void setMyaccounts(List<Account> myaccounts) { this.myaccounts = myaccounts; }
    public List<Account> getRecipientAccounts() { return recipientAccounts; }
    public void setRecipientAccounts(List<Account> recipientAccounts) { this.recipientAccounts = recipientAccounts; }
}
