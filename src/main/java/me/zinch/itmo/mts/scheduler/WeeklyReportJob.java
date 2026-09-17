package me.zinch.itmo.mts.scheduler;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zinch.itmo.mts.service.reporting.WeeklyReportEmailService;
import me.zinch.itmo.mts.service.reporting.WeeklyReportService;

@Slf4j
@RequiredArgsConstructor
public class WeeklyReportJob implements Job {

    public static final JobKey JOB_KEY = JobKey.jobKey("weekly-order-report");

    private final WeeklyReportService weeklyReportService;
    private final WeeklyReportEmailService weeklyReportEmailService;

    @Override
    public void execute(JobExecutionContext context) {
        var report = weeklyReportService.buildWeeklyOrderReport();
        weeklyReportEmailService.send(report);
        log.info("Weekly order report sent: totalOrders={}, successfulOrders={}, amount={}",
                report.totalOrders(), report.successfulOrders(), report.successfulOrdersAmount());
    }
}
