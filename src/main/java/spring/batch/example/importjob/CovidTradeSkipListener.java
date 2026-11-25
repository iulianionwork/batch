package spring.batch.example.importjob;

import org.springframework.batch.core.listener.SkipListener;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;
import org.springframework.batch.infrastructure.item.validator.ValidationException;
import spring.batch.example.data.TradeCsvRecord;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class CovidTradeSkipListener implements SkipListener<TradeCsvRecord,TradeCsvRecord> {

    Path skippedItemsFile;

    public CovidTradeSkipListener(String skippedItemsFile){
        this.skippedItemsFile = Paths.get(skippedItemsFile);
    }

    @Override
    public void onSkipInRead(Throwable throwable) {
        if (throwable instanceof FlatFileParseException exception ) {
            String rawLine = exception.getInput();
            int lineNumber = exception.getLineNumber();
            String skippedLine = "Read Line Error: " + lineNumber + " | " + rawLine + System.lineSeparator();
            try {
                Files.writeString(this.skippedItemsFile, skippedLine, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
            } catch (IOException e) {
                throw new RuntimeException("Unable to write skipped item " + skippedLine);
            }
        }
    }

    @Override
    public void onSkipInProcess(TradeCsvRecord row, Throwable throwable){
        if(throwable instanceof ValidationException exception ){

            String rawLine = "( " + row.direction() + ", " + row.date() + " )";
            String error = exception.getMessage();
            String skippedLine ="Invalid data on row: " + rawLine + " | "+ error + System.lineSeparator();
            try {
                Files.writeString(this.skippedItemsFile, skippedLine, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
            } catch (IOException e) {
                throw new RuntimeException("Unable to write skipped item " + skippedLine);
            }
        }


    }


}
