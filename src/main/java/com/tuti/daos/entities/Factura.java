package com.tuti.daos.entities;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import com.tuti.daos.common.exception.ConflictoNegocioException;
import com.tuti.daos.utilidades.EstadoFactura;

import jakarta.persistence.*;

@Entity
@Table(name = "facturas")
public class Factura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contrato_id", nullable = false)
    private Contrato contrato;

    @Convert(converter = com.tuti.daos.utilidades.YearMonthConverter.class)
    @Column(nullable = false, length = 7)
    private YearMonth periodo;

    @Column(length = 500)
    private String concepto;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDate fechaEmision;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal importe;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoFactura estado;

    @Column(name = "facturacion_mensual", nullable = false)
    private boolean facturacionMensual;

    protected Factura() {
    }

    public Factura(
            Contrato contrato,
            YearMonth periodo,
            String concepto,
            LocalDate fechaEmision,
            LocalDate fechaVencimiento,
            BigDecimal importe,
            EstadoFactura estado,
            boolean facturacionMensual) {

        this.contrato = contrato;
        this.periodo = periodo;
        this.concepto = concepto;
        this.fechaEmision = fechaEmision;
        this.fechaVencimiento = fechaVencimiento;
        this.importe = importe;
        this.estado = estado;
        this.facturacionMensual = facturacionMensual;
    }

    public Long getId() {
        return id;
    }

    public Contrato getContrato() {
        return contrato;
    }

    public YearMonth getPeriodo() {
        return periodo;
    }

    public String getConcepto() {
        return concepto;
    }

    public LocalDate getFechaEmision() {
        return fechaEmision;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public BigDecimal getImporte() {
        return importe;
    }

    public EstadoFactura getEstado() {
        return estado;
    }

    public boolean isFacturacionMensual() {
        return facturacionMensual;
    }

    public void setEstado(EstadoFactura estado) {
        this.estado = estado;
    }
    
    
    //ESTO NO IRIA ACA PERO PARA NO ROMPER LO DEL ALE LO DEJO DE MOMENTO
    public void verificarPagable() {
        if (estado != EstadoFactura.PENDIENTE && estado != EstadoFactura.VENCIDA) {
            throw new ConflictoNegocioException("La factura debe estar pendiente o vencida.");
        }
    }

    // La marcamos pagada desde el registro del pago, no desde un cambio de estado suelto.
    public void marcarPagada() {
        verificarPagable();
        estado = EstadoFactura.PAGADA;
    }
}
