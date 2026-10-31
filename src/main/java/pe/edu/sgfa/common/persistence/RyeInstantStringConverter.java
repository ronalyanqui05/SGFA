package pe.edu.sgfa.common.persistence;

import java.time.Instant;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class RyeInstantStringConverter implements AttributeConverter<Instant, String> {

    @Override
    public String convertToDatabaseColumn(Instant attributeYnq) {
        return attributeYnq == null ? null : attributeYnq.toString();
    }

    @Override
    public Instant convertToEntityAttribute(String dbDataYnq) {
        return dbDataYnq == null ? null : Instant.parse(dbDataYnq);
    }
}
