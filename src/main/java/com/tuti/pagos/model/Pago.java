package com.tuti.pagos.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.persistence.*;
import com.tuti.facturas.model.Factura;

@Entity
// No alcanza con validarlo en Java: la base tambien impide dos pagos para una factura.
@Table(name = "pagos", uniqueConstraints = @UniqueConstraint(
        name = "uk_pagos_factura", columnNames = "factura_id"))
public class Pago {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "factura_id", nullable = false)
    private Factura factura;

    @Column(name = "fecha_pago", nullable = false)
    private LocalDate fechaPago;

    @Enumerated(EnumType.STRING)
    @Column(name = "medio_pago", nullable = false, length = 20)
    private MedioPago medioPago;

    @Column(name = "importe_pagado", nullable = false, precision = 15, scale = 2)
    private BigDecimal importePagado;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal interes;

    // Este constructor vacio lo necesita JPA para recuperar los datos de la base.
    protected Pago() {}

    public Pago(Factura factura, LocalDate fechaPago, MedioPago medioPago,
            BigDecimal importePagado, BigDecimal interes) {
        this.factura = factura;
        this.fechaPago = fechaPago;
        this.medioPago = medioPago;
        this.importePagado = importePagado;
        this.interes = interes;
    }

    public Long getId() { return id; }
    public Factura getFactura() { return factura; }
    public LocalDate getFechaPago() { return fechaPago; }
    public MedioPago getMedioPago() { return medioPago; }
    public BigDecimal getImportePagado() { return importePagado; }
    public BigDecimal getInteres() { return interes; }
}
