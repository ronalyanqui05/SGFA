package pe.edu.sgfa.common.persistence;

import java.time.Instant;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class InstantStringConverter implements AttributeConverter<Instant, String> {

    @Override
    public String convertToDatabaseColumn(Instant attrbt) {
        return attrbt == null ? null : attrbt.toString();
    }

    @Override
    public Instant convertToEntityAttribute(String dbDt) {
        return dbDt == null ? null : Instant.parse(dbDt);
    }
}
