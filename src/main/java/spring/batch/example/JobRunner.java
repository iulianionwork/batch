package spring.batch.example;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class JobRunner implements CommandLineRunner {

    private final JobOperator jobOperator;
    private final Job importJob;
    private final Job reportJob;

    public JobRunner(JobOperator jobOperator, @Qualifier("importJob") Job importJob,@Qualifier("reportJob") Job reportJob){
        this.importJob = importJob;
        this.jobOperator = jobOperator;
        this.reportJob = reportJob;
    }
    @Override
    public void run(String... args) throws Exception {
        JobParameters importParameters = new JobParametersBuilder()
                .addString("input.file", "src/main/resources/input/covid_trade.csv")
                .addString("new.dir", "src/main/resources/processed")
                .addString("skip.file", "src/main/resources/output/skip-rows.csv")
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters();


        JobExecution importExecution = jobOperator.start(importJob, importParameters);
        if(importExecution.getStatus() != BatchStatus.COMPLETED){
            return;
        }




        JobParameters reportParameters = new JobParametersBuilder()
                .addString("output.file", "src/main/resources/output/covid-report.csv")
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters();

        jobOperator.start(reportJob, reportParameters);
    }



}
