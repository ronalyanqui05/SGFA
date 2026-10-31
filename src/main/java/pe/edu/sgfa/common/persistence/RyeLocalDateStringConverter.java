package pe.edu.sgfa.common.persistence;

import java.time.LocalDate;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class RyeLocalDateStringConverter implements AttributeConverter<LocalDate, String> {

    @Override
    public String convertToDatabaseColumn(LocalDate attributeYnq) {
        return attributeYnq == null ? null : attributeYnq.toString();
    }

    @Override
    public LocalDate convertToEntityAttribute(String dbDataYnq) {
        return dbDataYnq == null ? null : LocalDate.parse(dbDataYnq);
    }
}
