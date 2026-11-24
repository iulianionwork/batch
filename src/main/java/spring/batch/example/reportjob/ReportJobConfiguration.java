package spring.batch.example.reportjob;

import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.ChunkOrientedStepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.database.JdbcCursorItemReader;
import org.springframework.batch.infrastructure.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemWriter;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.support.JdbcTransactionManager;
import spring.batch.example.data.ReportCsvRecord;
import spring.batch.example.data.ReportDbRecord;

import javax.sql.DataSource;

@Configuration
public class ReportJobConfiguration {

    @Bean
    public JdbcTransactionManager transactionManager(DataSource dataSource){
        return new JdbcTransactionManager(dataSource);
    }

    @Bean
    public Step reportStep(
            JobRepository jobRepository,
            JdbcTransactionManager transactionManager,
            JdbcCursorItemReader<@NotNull ReportDbRecord> reportDataReader,
            ItemProcessor<@NotNull ReportDbRecord, @NotNull ReportCsvRecord> reportProcessor,
            FlatFileItemWriter<@NotNull ReportCsvRecord> reportDataWriter) {
        return new ChunkOrientedStepBuilder<@NotNull ReportDbRecord, @NotNull ReportCsvRecord>("reportStep", jobRepository, 1000)
                .reader(reportDataReader)
                .processor(reportProcessor)
                .writer(reportDataWriter)
                .transactionManager(transactionManager)
                .build();
    }

    @Bean
    public Job reprotJob(JobRepository jobRepository, Step reportStep) {
        return new JobBuilder("CovidReport", jobRepository)
                .start(reportStep)
                .build();
    }

    @Bean
    public JdbcCursorItemReader<@NotNull ReportDbRecord> reportDataReader(DataSource dataSource){
        String sql = """
                SELECT trade_date, direction, measure, SUM(value) as total, COUNT(*) as days
                FROM covid_trade
                WHERE country = 'All' AND direction = 'All' AND transport_mode = 'All'
                GROUP BY trade_date, direction, measure
                ORDER BY trade_date, direction, measure
                """;

        return new JdbcCursorItemReaderBuilder<@NotNull ReportDbRecord>()
                .name("reportDataReader")
                .dataSource(dataSource)
                .sql(sql)
                .rowMapper(((rs, rowNum) -> new ReportDbRecord(
                        rs.getInt("trade_date"),
                        rs.getString("direction"),
                        rs.getString("measure"),
                        rs.getLong("total"),
                        rs.getInt("days"))))
                .build();
    }

    @Bean
    public ItemProcessor<@NotNull ReportDbRecord, @NotNull ReportCsvRecord> reportProcessor(){
        return item ->  new ReportCsvRecord(
                item.year(),
                item.direction(),
                item.measure(),
                item.total(),
                item.days(),
                item.total()/ item.days());
    }

    @Bean
    @StepScope
    public FlatFileItemWriter<@NotNull ReportCsvRecord> reportWrite(@Value("#{jobParameters['input.file']}") String outputFile) {
        return new FlatFileItemWriterBuilder<@NotNull ReportCsvRecord>()
                .name("reportWrite")
                .resource(new FileSystemResource(outputFile))
                .headerCallback(header -> header.write("year,direction,measure,total,days,average_per_days"))
                .delimited()
                .delimiter(",")
                .fieldExtractor(field -> new Object[]{
                        field.year(),
                        field.direction(),
                        field.measure(),
                        field.total(),
                        field.days(),
                        field.averagePerDay()})
                .build();
    }


}
