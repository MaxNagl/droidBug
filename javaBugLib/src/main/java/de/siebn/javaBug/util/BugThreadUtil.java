package de.siebn.javaBug.util;

import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class BugThreadUtil {
    public static BugThreadUtil runOn = new BugThreadUtil();

    public final RunOn worker = new RunOnScheduledExecutorService(Executors.newSingleThreadScheduledExecutor());
    public final RunOn pool = new RunOnScheduledExecutorService(Executors.newScheduledThreadPool(0));
    public final RunOn current = new RunOnCurrentThread();

    public interface RunnableWithParameter<T> {
        void run(T parameter);
    }

    public static class Cancellable {
        private volatile boolean canceled;

        public void cancel() {
            canceled = true;
        }
    }

    public static abstract class RunOn {
        public abstract void async(Runnable runnable);

        public abstract <T> T sync(Callable<T> callable);

        public abstract void delayed(int delayMs, Runnable runnable);

        public abstract <T> T delayedSync(int delayMs, Callable<T> callable);

        public Cancellable repeat(long count, final int delay, final Runnable runnable) {
            return repeatIndexed(count, delay, new RunnableWithParameter<Long>() {
                @Override
                public void run(Long index) {
                    runnable.run();
                }
            });
        }

        public Cancellable repeatIndexed(final long count, final int delay, final RunnableWithParameter<Long> runnable) {
            final AtomicLong index = new AtomicLong(0);
            final Cancellable canceled = new Cancellable() {
                @SuppressWarnings("unused") public AtomicLong current = index; // For debugging.
                @Override
                public String toString() {
                    return "{Repeat count: " + count + " delay: " + delay + " ms}";
                }
            };
            final long max = count < 0 ? Long.MAX_VALUE : count;
            delayed(delay, new Runnable() {
                @Override
                public void run() {
                    if (!canceled.canceled) {
                        runnable.run(index.get());
                        if (index.incrementAndGet() < max) {
                            delayed(delay, this);
                        }
                    }
                }
            });
            return canceled;
        }

        public <T> Cancellable repeatRandom(long count, final int delay, final RunnableWithParameter<T> runnable, final T... values) {
            final Random r = new Random();
            return repeatIndexed(count, delay, new RunnableWithParameter<Long>() {
                @Override
                public void run(Long index) {
                    runnable.run(values[r.nextInt(values.length)]);
                }
            });
        }

        public <T> Cancellable repeatCycle(long count, final int delay, final RunnableWithParameter<T> runnable, final T... values) {
            return repeatIndexed(count, delay, new RunnableWithParameter<Long>() {
                @Override
                public void run(Long index) {
                    runnable.run(values[(int) (index % (long) values.length)]);
                }
            });
        }

        protected static <T> T get(Future<T> future) {
            try {
                return future.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        protected static <T> T get(Callable<T> callable) {
            try {
                return callable.call();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        protected void sleep(int millis) {
            try {
                Thread.sleep(millis, 0);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private static class RunOnScheduledExecutorService extends RunOn {
        private final ScheduledExecutorService workerExecutor;

        private RunOnScheduledExecutorService(ScheduledExecutorService workerExecutor) {
            this.workerExecutor = workerExecutor;
        }

        @Override
        public void async(Runnable runnable) {
            workerExecutor.submit(runnable);
        }

        @Override
        public <T> T sync(Callable<T> callable) {
            return get(workerExecutor.submit(callable));
        }

        @Override
        public void delayed(int delayMs, Runnable runnable) {
            workerExecutor.schedule(runnable, delayMs, TimeUnit.MILLISECONDS);
        }

        @Override
        public <T> T delayedSync(int delayMs, Callable<T> callable) {
            return get(workerExecutor.schedule(callable, delayMs, TimeUnit.MILLISECONDS));
        }
    }

    private static class RunOnCurrentThread extends RunOn {
        @Override
        public void async(Runnable runnable) {
            runnable.run();
        }

        @Override
        public <T> T sync(Callable<T> callable) {
            try {
                return callable.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void delayed(int delayMs, Runnable runnable) {
            sleep(delayMs);
            runnable.run();
        }

        @Override
        public <T> T delayedSync(int delayMs, Callable<T> callable) {
            sleep(delayMs);
            return get(callable);
        }
    }
}
