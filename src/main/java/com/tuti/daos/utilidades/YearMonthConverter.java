package com.tuti.daos.utilidades;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.YearMonth;

@Converter
public class YearMonthConverter
        implements AttributeConverter<YearMonth, String> {

    @Override
    public String convertToDatabaseColumn(YearMonth periodo) {
        return periodo != null ? periodo.toString() : null;
    }

    @Override
    public YearMonth convertToEntityAttribute(String valor) {
        return valor != null ? YearMonth.parse(valor) : null;
    }
}
