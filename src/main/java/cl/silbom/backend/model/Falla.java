package cl.silbom.backend.model;

import jakarta.persistence.*;

/** Una falla identificada, independiente del número de técnicos participantes. */
@Entity
@Table(name = "fallas", uniqueConstraints = @UniqueConstraint(columnNames = {"planta_id", "maquina_id", "referencia"}))
public class Falla {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "maquina_id", nullable = false, length = 20, updatable = false) private String maquinaId;
    @Column(name = "planta_id", nullable = false, length = 50, updatable = false) private String plantaId;
    @Column(nullable = false, length = 80, updatable = false) private String referencia;
    protected Falla() {}
    public Falla(String maquinaId, String plantaId, String referencia) {
        this.maquinaId = maquinaId; this.plantaId = plantaId; this.referencia = referencia;
    }
    public Long getId() { return id; }
}
