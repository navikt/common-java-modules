package no.nav.common.jetty.utils;

public class ChainedRunnables implements Runnable {

    private final Runnable task;

    private final Runnable next;

    public ChainedRunnables(Runnable task) {
        this(task, null);
    }

    public ChainedRunnables then(Runnable nextTask) {
        if (next != null) {
            return new ChainedRunnables(new ChainedRunnables(this.task, this.next), nextTask);
        }
        return new ChainedRunnables(this.task, nextTask);
    }

    private ChainedRunnables(Runnable task, Runnable next) {
        this.task = task;
        this.next = next;
    }

    @Override
    public void run() {
        task.run();
        if (next != null) {
            next.run();
        }
    }

}
