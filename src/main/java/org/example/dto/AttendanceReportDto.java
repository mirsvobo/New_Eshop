package org.example.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceReportDto {
    private LocalDate date;
    private LocalDateTime clockIn;
    private LocalDateTime clockOut;
    private Duration totalBreakTime;
    private Duration netWorkTime;

    private boolean missingClockOut;
    private boolean cappedAtMax;

    public String formatDuration(Duration duration) {
        if (duration == null) return "00:00";
        long s = duration.abs().getSeconds();
        return String.format("%02d:%02d", s / 3600, (s % 3600) / 60);
    }
}