package cl.silbom.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * QUÉ HACE:
 * Entidad JPA que representa una máquina o equipo industrial dentro de la Planta San Joaquín.
 * Mantiene el inventario, los contadores de horas operativas, fallas, MTBF y el estado
 * concurrente de bloqueo (Multi-LOTO).
 *
 * POR QUÉ ESTÁ ESCRITO ASÍ (CONTROL 8 DE CIBERSEGURIDAD: ENCAPSULAMIENTO ESTRICTO):
 * - Todos los atributos son 'private' y se prohíbe el uso de Lombok @Data o @Setter genéricos.
 * - Los campos críticos como 'bloqueada', 'bloqueosActivos', 'mtbf' e 'id' NO tienen setters públicos.
 * - Solo pueden mutar mediante métodos de dominio con validación de reglas de negocio
 *   (ej: registrarEntradaBloqueo, registrarSalidaBloqueo, recalcularMtbf).
 * - El constructor vacío es 'protected' cumpliendo con la especificación JPA sin exponerlo a clientes externos.
 */
@Entity
@Table(name = "maquinas")
public class Maquina {

    @Id
    @Column(name = "id", length = 20, nullable = false, updatable = false)
    private String id;

    @Column(name = "nombre", length = 100, nullable = false)
    private String nombre;

    @Column(name = "area", length = 100, nullable = false)
    private String area;

    @Column(name = "icono", length = 10, nullable = false)
    private String icono;

    @Column(name = "horas_operativas", nullable = false)
    private Double horasOperativas;

    @Column(name = "fallas_correctivas", nullable = false)
    private Integer fallasCorrectivas;

    @Column(name = "mtbf", nullable = false)
    private Double mtbf;

    @Column(name = "bloqueada", nullable = false)
    private Boolean bloqueada;

    @Column(name = "bloqueos_activos", nullable = false)
    private Integer bloqueosActivos;

    @Column(name = "planta_id", length = 50, nullable = false)
    private String plantaId;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_modificacion", nullable = false)
    private LocalDateTime fechaModificacion;

    /**
     * Constructor por defecto requerido obligatoriamente por la especificación de JPA.
     * Se declara 'protected' para impedir instanciaciones vacías e incompletas desde fuera del paquete.
     */
    protected Maquina() {
    }

    /**
     * Constructor de dominio para dar de alta una nueva máquina con sus valores iniciales completos.
     */
    public Maquina(String id, String nombre, String area, String icono, Double horasOperativas, Integer fallasCorrectivas, String plantaId) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El ID de la máquina no puede ser nulo ni vacío");
        }
        this.id = id;
        this.nombre = nombre;
        this.area = area;
        this.icono = (icono != null && !icono.isBlank()) ? icono : "⚙️";
        this.horasOperativas = (horasOperativas != null && horasOperativas >= 0) ? horasOperativas : 0.0;
        this.fallasCorrectivas = (fallasCorrectivas != null && fallasCorrectivas >= 0) ? fallasCorrectivas : 0;
        this.bloqueada = false;
        this.bloqueosActivos = 0;
        this.plantaId = (plantaId != null && !plantaId.isBlank()) ? plantaId : "PLANTA-SAN-JOAQUIN";
        this.fechaCreacion = LocalDateTime.now();
        this.fechaModificacion = LocalDateTime.now();
        this.recalcularMtbf();
    }

    /**
     * QUÉ HACE:
     * Calcula la métrica industrial MTBF (Mean Time Between Failures) y la redondea a 1 decimal.
     *
     * POR QUÉ ESTÁ ESCRITO ASÍ:
     * Fórmula oficial: MTBF = horasOperativas / fallasCorrectivas.
     * Si fallasCorrectivas == 0, el MTBF equivale a las horas operativas completas (sin división por cero).
     * Se usa Math.round(valor * 10.0) / 10.0 para garantizar exactitud de un decimal.
     */
    public void recalcularMtbf() {
        if (this.fallasCorrectivas == null || this.fallasCorrectivas <= 0) {
            this.mtbf = this.horasOperativas != null ? Math.round(this.horasOperativas * 10.0) / 10.0 : 0.0;
        } else {
            double valor = this.horasOperativas / this.fallasCorrectivas;
            this.mtbf = Math.round(valor * 10.0) / 10.0;
        }
    }

    /**
     * QUÉ HACE:
     * Registra el ingreso de un nuevo técnico aplicando un candado LOTO sobre la máquina.
     *
     * POR QUÉ ESTÁ ESCRITO ASÍ (REGLA MULTI-LOTO):
     * - Incrementa en +1 el contador 'bloqueosActivos'.
     * - Pone la máquina en estado 'bloqueada = true'.
     * - Si la intervención es CORRECTIVA (falla no programada), incrementa en +1 el contador
     *   de fallasCorrectivas y recalcula inmediatamente el MTBF. Si es PREVENTIVA, no penaliza.
     */
    public void registrarEntradaBloqueo(TipoIntervencion tipo) {
        if (this.bloqueosActivos == null) {
            this.bloqueosActivos = 0;
        }
        this.bloqueosActivos++;
        this.bloqueada = true;

        if (tipo == TipoIntervencion.CORRECTIVA) {
            if (this.fallasCorrectivas == null) {
                this.fallasCorrectivas = 0;
            }
            this.fallasCorrectivas++;
            this.recalcularMtbf();
        }
        this.fechaModificacion = LocalDateTime.now();
    }

    /**
     * QUÉ HACE:
     * Registra el retiro del candado LOTO por parte de un técnico que culminó su labor.
     *
     * POR QUÉ ESTÁ ESCRITO ASÍ (REGLA CRÍTICA MULTI-LOTO):
     * - Reduce en 1 el contador 'bloqueosActivos'.
     * - Si aún quedan técnicos trabajando (bloqueosActivos > 0), la máquina DEBE permanecer
     *   bloqueada ('bloqueada = true') para salvar la vida de los técnicos restantes.
     * - SOLO cuando 'bloqueosActivos == 0', la máquina se declara desbloqueada ('bloqueada = false').
     */
    public void registrarSalidaBloqueo() {
        if (this.bloqueosActivos != null && this.bloqueosActivos > 0) {
            this.bloqueosActivos--;
        }
        if (this.bloqueosActivos == null || this.bloqueosActivos == 0) {
            this.bloqueosActivos = 0;
            this.bloqueada = false;
        }
        this.fechaModificacion = LocalDateTime.now();
    }

    /**
     * QUÉ HACE:
     * Actualiza la información técnica y operativa del equipo permitida desde la API REST.
     *
     * POR QUÉ ESTÁ ESCRITO ASÍ:
     * Evita Mass Assignment protegiendo los estados 'bloqueada' y 'bloqueosActivos', y
     * recalcula automáticamente el MTBF tras modificar horas o fallas.
     */
    public void actualizarDatos(String nombre, String area, String icono, Double horasOperativas, Integer fallasCorrectivas) {
        this.nombre = nombre;
        this.area = area;
        if (icono != null && !icono.isBlank()) {
            this.icono = icono;
        }
        if (horasOperativas != null && horasOperativas >= 0) {
            this.horasOperativas = horasOperativas;
        }
        if (fallasCorrectivas != null && fallasCorrectivas >= 0) {
            this.fallasCorrectivas = fallasCorrectivas;
        }
        this.recalcularMtbf();
        this.fechaModificacion = LocalDateTime.now();
    }

    // ====================================================================
    // GETTERS LIMPIOS (Solo lectura, sin setters públicos de mutación libre)
    // ====================================================================

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getArea() {
        return area;
    }

    public String getIcono() {
        return icono;
    }

    public Double getHorasOperativas() {
        return horasOperativas;
    }

    public Integer getFallasCorrectivas() {
        return fallasCorrectivas;
    }

    public Double getMtbf() {
        return mtbf;
    }

    public Boolean getBloqueada() {
        return bloqueada;
    }

    public Integer getBloqueosActivos() {
        return bloqueosActivos;
    }

    public String getPlantaId() {
        return plantaId;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getFechaModificacion() {
        return fechaModificacion;
    }
}
