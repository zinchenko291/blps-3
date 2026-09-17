package me.zinch.itmo.mts.config;

import java.util.TimeZone;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.quartz.autoconfigure.SchedulerFactoryBeanCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

import me.zinch.itmo.mts.scheduler.WeeklyReportJob;

@Configuration
public class QuartzConfig {

    @Bean
    SchedulerFactoryBeanCustomizer quartzJobFactoryCustomizer(AutowireCapableBeanFactory beanFactory) {
        return schedulerFactoryBean -> schedulerFactoryBean.setJobFactory(new SpringAutowiringJobFactory(beanFactory));
    }

    @Bean
    JobDetail weeklyReportJobDetail() {
        return JobBuilder.newJob(WeeklyReportJob.class)
                .withIdentity(WeeklyReportJob.JOB_KEY)
                .storeDurably()
                .build();
    }

    @Bean
    Trigger weeklyReportTrigger(JobDetail weeklyReportJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(weeklyReportJobDetail)
                .withIdentity("weekly-order-report-trigger")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 17 ? * FRI")
                        .inTimeZone(TimeZone.getTimeZone("Europe/Moscow")))
                .build();
    }

    private static final class SpringAutowiringJobFactory extends SpringBeanJobFactory {

        private final AutowireCapableBeanFactory beanFactory;

        private SpringAutowiringJobFactory(AutowireCapableBeanFactory beanFactory) {
            this.beanFactory = beanFactory;
        }

        @Override
        protected Object createJobInstance(TriggerFiredBundle bundle) throws Exception {
            return beanFactory.autowire(bundle.getJobDetail().getJobClass(),
                    AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR, false);
        }
    }
}
