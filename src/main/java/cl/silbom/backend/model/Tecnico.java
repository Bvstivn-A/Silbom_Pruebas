package cl.silbom.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "tecnicos", uniqueConstraints = @UniqueConstraint(columnNames = {"planta_id", "username"}))
public class Tecnico {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 50, updatable = false)
    private String username;
    @Column(nullable = false, length = 80)
    private String nombre;
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private Rol rol;
    @Column(nullable = false)
    private boolean activo = true;
    @Column(name = "planta_id", nullable = false, length = 50, updatable = false)
    private String plantaId;

    protected Tecnico() {}
    public Tecnico(String username, String nombre, String hash, Rol rol, String plantaId) {
        this.username = username; this.nombre = nombre; this.passwordHash = hash;
        this.rol = rol; this.plantaId = plantaId;
    }
    public void actualizar(String nombre, Rol rol, String hash) {
        this.nombre = nombre; this.rol = rol;
        if (hash != null) this.passwordHash = hash;
    }
    public void desactivar() { this.activo = false; }
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getNombre() { return nombre; }
    public String getPasswordHash() { return passwordHash; }
    public Rol getRol() { return rol; }
    public boolean isActivo() { return activo; }
    public String getPlantaId() { return plantaId; }
}
