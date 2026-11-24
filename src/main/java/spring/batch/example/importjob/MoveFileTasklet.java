package spring.batch.example.importjob;

import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;


public class MoveFileTasklet implements Tasklet {

    private final Path sourceFile;
    private final Path newDirectory;

    public MoveFileTasklet(Path sourceFile, Path newDirectory){
        this.newDirectory = newDirectory;
        this.sourceFile = sourceFile;
    }

    @Override
    public @Nullable RepeatStatus execute(@NotNull StepContribution contribution, @NotNull ChunkContext chunkContext) throws Exception {
        Files.createDirectories(newDirectory);
        Path directory = newDirectory.resolve(sourceFile.getFileName());
        Files.move(sourceFile,directory, StandardCopyOption.REPLACE_EXISTING);

        return RepeatStatus.FINISHED;
    }
}
