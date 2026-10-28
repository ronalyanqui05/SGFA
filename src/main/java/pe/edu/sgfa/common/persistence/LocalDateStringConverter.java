package pe.edu.sgfa.common.persistence;

import java.time.LocalDate;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class LocalDateStringConverter implements AttributeConverter<LocalDate, String> {

    @Override
    public String convertToDatabaseColumn(LocalDate attrbt) {
        return attrbt == null ? null : attrbt.toString();
    }

    @Override
    public LocalDate convertToEntityAttribute(String dbDt) {
        return dbDt == null ? null : LocalDate.parse(dbDt);
    }
}
