package no.nav.fo.feed.consumer;

import lombok.SneakyThrows;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.util.HashMap;
import java.util.Map;

import static no.nav.fo.feed.util.UrlUtils.asUrl;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

public class FeedPoller implements Job {

    private static Scheduler scheduler;
    private static Map<String, Runnable> jobs = new HashMap<>();

    static {
        try {
            scheduler = new StdSchedulerFactory().getScheduler();
            scheduler.start();
        } catch (SchedulerException e) {
            throw new RuntimeException("Could not create schduler", e);
        }
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobKey jobkey = context.getJobDetail().getKey();
        String name = asUrl(jobkey.getGroup(), jobkey.getName());
        jobs.getOrDefault(name, () -> {}).run();
    }

    @SneakyThrows
    public static void createScheduledJob(String name, String group, String cron, Runnable jobImpl) {
        if (isNotBlank(cron)) {
            JobDetail job = newJob(FeedPoller.class)
                    .withIdentity(name, group)
                    .build();

            CronTrigger trigger = newTrigger()
                    .withIdentity(name, group)
                    .withSchedule(cronSchedule(cron))
                    .build();

            scheduler.scheduleJob(job, trigger);

            String jobname = asUrl(group, name);
            jobs.putIfAbsent(jobname, jobImpl);
        }
    }
}
