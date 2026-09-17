package me.zinch.itmo.mts.service.reporting;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import me.zinch.itmo.mts.scheduler.WeeklyReportJob;
import me.zinch.itmo.mts.service.ServiceException;

@Service
@RequiredArgsConstructor
public class WeeklyReportScheduler {

    private final Scheduler scheduler;

    @PreAuthorize("hasAuthority('WEEKLY_REPORT_RUN')")
    public void runNow() {
        try {
            scheduler.triggerJob(WeeklyReportJob.JOB_KEY);
        } catch (SchedulerException exception) {
            throw new ServiceException("Не удалось запустить недельный отчёт", exception);
        }
    }
}
