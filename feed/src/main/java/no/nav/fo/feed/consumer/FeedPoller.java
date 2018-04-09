package no.nav.fo.feed.consumer;

import lombok.SneakyThrows;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.util.HashMap;
import java.util.Map;

import static no.nav.fo.feed.util.UrlUtils.asUrl;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

@DisallowConcurrentExecution
public class FeedPoller implements Job {

    private static Scheduler scheduler;
    private static Map<String, Runnable> jobs = new HashMap<>();

    @SneakyThrows
    public synchronized static Scheduler getScheduler() {
        if (scheduler == null) {
            scheduler = new StdSchedulerFactory().getScheduler();
            scheduler.start();
        }
        return scheduler;
    }

    @Override
    public void execute(JobExecutionContext context) {
        JobKey jobkey = context.getJobDetail().getKey();
        String name = asUrl(jobkey.getGroup(), jobkey.getName());
        jobs.getOrDefault(name, () -> {}).run();
    }

    @SneakyThrows
    public static void createScheduledJob(String name, String group, FeedConsumerConfig.ScheduleCreator scheduleCreator, Runnable jobImpl) {
        if (scheduleCreator != null) {
            JobDetail job = newJob(FeedPoller.class)
                    .withIdentity(name, group)
                    .build();

            Trigger trigger = newTrigger()
                    .withIdentity(name, group)
                    .withSchedule(scheduleCreator.scheduleBuilder)
                    .build();

            getScheduler().scheduleJob(job, trigger);

            String jobname = asUrl(group, name);
            jobs.putIfAbsent(jobname, jobImpl);
        }
    }

    @SneakyThrows
    public static void shutdown() {
        if (scheduler != null) {
            scheduler.shutdown();
            jobs.clear();
            scheduler = null;
        }
    }

}
