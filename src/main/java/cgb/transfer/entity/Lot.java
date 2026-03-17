package cgb.transfer.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class Lot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate dateLancement;

    @Enumerated(EnumType.STRING)
    private TransferStatus etat;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getDateLancement() { return dateLancement; }
    public void setDateLancement(LocalDate dateLancement) { this.dateLancement = dateLancement; }

    public TransferStatus getEtat() { return etat; }
    public void setEtat(TransferStatus etat) { this.etat = etat; }
}
