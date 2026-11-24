package spring.batch.example.importjob;

import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.ChunkOrientedStepBuilder;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.infrastructure.item.support.CompositeItemProcessor;
import org.springframework.batch.infrastructure.item.support.builder.CompositeItemProcessorBuilder;
import org.springframework.batch.infrastructure.item.validator.ValidatingItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;
import spring.batch.example.data.TradeCsvRecord;
import spring.batch.example.data.TradeDbRecord;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.time.LocalDate;


@Configuration
public class ImportJobConfiguration {


    @Bean
    public Step moveFileStep(JobRepository jobRepository,
                             PlatformTransactionManager transactionManager,
                             MoveFileTasklet moveFileTasklet) {
        return new StepBuilder("moveFileStep", jobRepository)
                .tasklet(moveFileTasklet, transactionManager)
                .build();
    }

    @Bean
    public Step importStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemReader<@NotNull TradeCsvRecord> tradeReader,
            CompositeItemProcessor<@NotNull TradeCsvRecord, @NotNull TradeDbRecord> tradeValidationProcessor,
            ItemWriter<@NotNull TradeDbRecord> tradeWriter) {
        return new ChunkOrientedStepBuilder<@NotNull TradeCsvRecord, @NotNull TradeDbRecord>("importStep", jobRepository, 1000)
                .reader(tradeReader)
                .processor(tradeValidationProcessor)
                .writer(tradeWriter)
                .transactionManager(transactionManager)
                .build();
    }

    @Bean
    public Job importJob(JobRepository jobRepository, Step importStep, Step moveFileStep) {
        return new JobBuilder("CovidTrade", jobRepository)
                .start(importStep)
                .next(moveFileStep)
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<@NotNull TradeCsvRecord> tradeReader(@Value("#{jobParameters['input.file']}") String inputFile) {
        return new FlatFileItemReaderBuilder<@NotNull TradeCsvRecord>()
                .name("tradeReader")
                .resource(new FileSystemResource(inputFile))
                .linesToSkip(1)
                .delimited()
                .names("direction",
                        "year",
                        "date",
                        "weekday",
                        "country",
                        "commodity",
                        "transportMode",
                        "measure",
                        "value",
                        "cumulative")
                .fieldSetMapper(fieldSet -> new TradeCsvRecord(
                        fieldSet.readString("direction"),
                        fieldSet.readInt("year"),
                        LocalDate.parse(fieldSet.readString("date")),
                        fieldSet.readString("weekend"),
                        fieldSet.readString("country"),
                        fieldSet.readString("commodity"),
                        fieldSet.readString("transportMode"),
                        fieldSet.readString("measure"),
                        fieldSet.readLong("value"),
                        fieldSet.readLong("cumulative")))
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<@NotNull TradeDbRecord> tradeWriter(DataSource dataSource) {
        String sql = """ 
                INSERT INTO covid_trade
                VALUES (:direction, :date, :country, :commodity, :transportMode, :measure, :value, :cumulative)
                ON CONFLICT ON CONSTRAINT uq_trade DO NOTHING
                """;
        return new JdbcBatchItemWriterBuilder<@NotNull TradeDbRecord>()
                .dataSource(dataSource)
                .sql(sql)
                .assertUpdates(false)
                .beanMapped()
                .build();
    }

    @Bean
    public CompositeItemProcessor<@NotNull TradeCsvRecord, @NotNull TradeDbRecord> tradeProcessor(){
        ValidatingItemProcessor<@NotNull TradeCsvRecord> validate = new ValidatingItemProcessor<>(new CovidTradeValidator());
        validate.setFilter(true);

        ItemProcessor<@NotNull TradeCsvRecord, @NotNull TradeDbRecord> cleanData = row -> new TradeDbRecord(row.direction(),
                row.date(),
                row.country(),
                row.commodity(),
                row.transportMode(),
                row.measure(),
                row.value(),
                row.cumulative());

        return new CompositeItemProcessorBuilder<@NotNull TradeCsvRecord, @NotNull TradeDbRecord>()
                .delegates(validate, cleanData)
                .build();
    }

    @Bean
    @StepScope
    public MoveFileTasklet moveFileTasklet(
            @Value("#{jobParameters['input.file']}") String inputFile,
            @Value("#{jobParameters['new.dir']}") String newDirectory){
        return new MoveFileTasklet(Path.of(inputFile),Path.of(newDirectory));
    }

}

