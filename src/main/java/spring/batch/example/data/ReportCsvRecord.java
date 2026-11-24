package spring.batch.example.data;

public record ReportCsvRecord(
        int year,
        String direction,
        String measure,
        long total,
        int days,
        long averagePerDay) {
}
