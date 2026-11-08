package com.tuti.daos.entities;

import java.time.YearMonth;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
// En Java usamos YearMonth y en la base lo guardamos como texto, por ejemplo "2026-11".
public class PeriodoConverter implements AttributeConverter<YearMonth, String> {
    @Override
    public String convertToDatabaseColumn(YearMonth periodo) {
        return periodo == null ? null : periodo.toString();
    }

    @Override
    public YearMonth convertToEntityAttribute(String periodo) {
        return periodo == null ? null : YearMonth.parse(periodo);
    }
}
