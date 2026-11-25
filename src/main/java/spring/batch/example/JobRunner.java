package spring.batch.example;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JobRunner implements ApplicationRunner {

    private final JobOperator jobOperator;
    private final Job importJob;
    private final Job reportJob;

    public JobRunner(JobOperator jobOperator, @Qualifier("importJob") Job importJob,@Qualifier("reportJob") Job reportJob){
        this.importJob = importJob;
        this.jobOperator = jobOperator;
        this.reportJob = reportJob;
    }
    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<String> jobs = args.getNonOptionArgs();

        if(jobs.isEmpty()){
            System.out.println("What job need to run: import or export?");
            return;
        }

        switch (jobs.get(0)){
            case "import" -> runImport();
            case "export" -> runExport();
            default -> System.out.println("Invalid job name use \"import\" or \"export\".");
        }


    }

    private void runImport() throws Exception{
        JobParameters importParameters = new JobParametersBuilder()
                .addString("input.file", "src/main/resources/input/covid_trade.csv")
                .addString("new.dir", "src/main/resources/processed")
                .addString("skip.file", "src/main/resources/output/skip-rows.csv")
                .toJobParameters();
         jobOperator.start(importJob, importParameters);

    }

    private void runExport() throws Exception{
        JobParameters reportParameters = new JobParametersBuilder()
                .addString("output.file", "src/main/resources/output/covid-report.csv")
                .toJobParameters();

        jobOperator.start(reportJob, reportParameters);
    }



}
