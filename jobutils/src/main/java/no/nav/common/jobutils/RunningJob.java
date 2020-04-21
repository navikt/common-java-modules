package no.nav.common.jobutils;

public class RunningJob {
    private String jobId;
    private String podName;

    RunningJob(String jobId, String podName) {
        this.jobId = jobId;
        this.podName = podName;
    }

    public String getJobId() {
        return jobId;
    }

    public String getPodName() {
        return podName;
    }
}
