package spring.batch.example.data;

public record ReportDbRecord(
        int year,
        String direction,
        String measure,
        long total,
        int days) {
}
