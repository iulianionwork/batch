package spring.batch.example.importjob;

import org.jetbrains.annotations.NotNull;
import org.springframework.batch.infrastructure.item.validator.ValidationException;
import org.springframework.batch.infrastructure.item.validator.Validator;

import spring.batch.example.data.TradeCsvRecord;

import java.util.Set;

public class CovidTradeValidator implements Validator<@NotNull TradeCsvRecord> {

    private static final Set<String> DIRECTIONS = Set.of("Imports", "Exports", "Reimports");
    private static final Set<String> MEASURES = Set.of("Tonnes", "$");

    @Override
    public void validate(TradeCsvRecord value) throws ValidationException {
        if(!DIRECTIONS.contains(value.direction())){
            throw new ValidationException("Invalid direction: " + value.direction());
        }
        if(!MEASURES.contains(value.measure())){
            throw new ValidationException("Invalid measure: " + value.measure());
        }
        if(value.value() < 0 || value.cumulative() < 0){
            throw new ValidationException("Negative value on date: " +value.date());
        }
        if(isBlank(value.country()) || isBlank(value.commodity()) || isBlank(value.transportMode())){
            throw new ValidationException("Invalid row on date: " +value.date());
        }
    }

    private boolean isBlank(String s){
        return s == null || s.isBlank();
    }
}
