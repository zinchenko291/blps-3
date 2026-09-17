package me.zinch.itmo.mts.web.reporting;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import me.zinch.itmo.mts.service.reporting.WeeklyReportScheduler;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class WeeklyReportController {

    private final WeeklyReportScheduler weeklyReportScheduler;

    @PostMapping("/weekly/run")
    public ResponseEntity<Void> runWeeklyReport() {
        weeklyReportScheduler.runNow();
        return ResponseEntity.accepted().build();
    }
}
