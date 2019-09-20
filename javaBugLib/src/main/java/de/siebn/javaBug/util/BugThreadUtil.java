package de.siebn.javaBug.util;

import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class BugThreadUtil {
    public static BugThreadUtil INSTANCE = new BugThreadUtil();

    public final static RunOn worker = new RunOnScheduledExecutorService(Executors.newSingleThreadScheduledExecutor());
    public final static RunOn pool = new RunOnScheduledExecutorService(Executors.newScheduledThreadPool(0));
    public final static RunOn current = new RunOnCurrentThread();

    interface RunnableWithParameter<T> {
        void run(T parameter);
    }

    interface Cancellable {
        void cancel();
    }

    public static abstract class RunOn {
        public abstract void async(Runnable runnable);

        public abstract <T> T sync(Callable<T> callable);

        public abstract void delayed(int delayMs, Runnable runnable);

        public abstract <T> T delayedSync(int delayMs, Callable<T> callable);

        public Cancellable repeat(long count, final int delay, final Runnable runnable) {
            final AtomicLong c = new AtomicLong(0);
            final long max = count == 0 ? Long.MAX_VALUE : count;
            delayed(delay, new Runnable() {
                @Override
                public void run() {
                    runnable.run();
                    if (c.incrementAndGet() < max) {
                        delayed(delay, this);
                    }
                }
            });
            return getCancellable(c);
        }

        public Cancellable repeatIndex(long count, final int delay, final RunnableWithParameter<Long> runnable) {
            final AtomicLong c = new AtomicLong(0);
            final long max = count == 0 ? Long.MAX_VALUE : count;
            delayed(delay, new Runnable() {
                @Override
                public void run() {
                    runnable.run(c.get());
                    if (c.incrementAndGet() < max) {
                        delayed(delay, this);
                    }
                }
            });
            return getCancellable(c);
        }

        public <T> Cancellable repeatRandom(long count, final int delay, final RunnableWithParameter<T> runnable, final T... values) {
            final AtomicLong c = new AtomicLong(0);
            final long max = count == 0 ? Long.MAX_VALUE : count;
            final Random r = new Random();
            delayed(delay, new Runnable() {
                @Override
                public void run() {
                    runnable.run(values[r.nextInt(values.length)]);
                    if (c.incrementAndGet() < max) {
                        delayed(delay, this);
                    }
                }
            });
            return getCancellable(c);
        }

        public <T> Cancellable repeatCycle(long count, final int delay, final RunnableWithParameter<T> runnable, final T... values) {
            final AtomicLong c = new AtomicLong(0);
            final long max = count == 0 ? Long.MAX_VALUE : count;
            delayed(delay, new Runnable() {
                @Override
                public void run() {
                    runnable.run(values[(int) (c.get() % values.length)]);
                    if (c.incrementAndGet() < max) {
                        delayed(delay, this);
                    }
                }
            });
            return getCancellable(c);
        }

        private Cancellable getCancellable(final AtomicLong ai) {
            return new Cancellable() {
                @Override
                public void cancel() {
                    ai.set(Long.MAX_VALUE);
                }
            };
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
