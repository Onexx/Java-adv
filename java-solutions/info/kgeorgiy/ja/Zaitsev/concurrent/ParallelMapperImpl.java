package info.kgeorgiy.ja.Zaitsev.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation of {@link ParallelMapper} interface.
 *
 * @author Zaitsev Ilya
 */
public class ParallelMapperImpl implements ParallelMapper {
    private final Queue<Runnable> queue;
    private final List<Thread> threads;

    private static class Counter {
        private int counter = 0;

        public int getCounter() {
            return counter;
        }

        public void increase() {
            counter++;
        }
    }

    /**
     * Creates {@code threadsAmount} of worker threads that can be used for parallelization.
     */
    public ParallelMapperImpl(int threadsAmount) {
        if (threadsAmount < 1) {
            throw new IllegalArgumentException("Threads amount couldn't be less than 1");
        }
        queue = new ArrayDeque<>();
        final Runnable runnable = () -> {
            while (!Thread.currentThread().isInterrupted()) {
                final Runnable task;
                synchronized (queue) {
                    while (queue.isEmpty()) {
                        try {
                            queue.wait();
                        } catch (InterruptedException ignored) {
                            return;
                        }
                    }
                    task = queue.poll();
                }
                task.run();
            }
        };
        threads = Stream.generate(() -> new Thread(runnable)).limit(threadsAmount).collect(Collectors.toList());
        threads.forEach(Thread::start);
    }

    /**
     * Maps function {@code f} over specified {@code args}.
     * Mapping for each element performs in parallel.
     *
     * @throws InterruptedException if calling thread was interrupted
     */
    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        List<R> results = new ArrayList<>(Collections.nCopies(args.size(), null));
        final Counter counter = new Counter();
        for (int i = 0; i < args.size(); i++) {
            final int ti = i;
            synchronized (queue) {
                queue.add(
                        () -> {
                            results.set(ti, f.apply(args.get(ti)));
                            synchronized (counter) {
                                counter.increase();
                                if (counter.getCounter() == args.size()) {
                                    counter.notify();
                                }
                            }
                        }
                );

                queue.notify();
            }
        }
        synchronized (counter) {
            while (counter.getCounter() < args.size()) {
                counter.wait();
            }
        }
        return results;
    }

    /**
     * Stops all threads. All unfinished mappings leave in undefined state.
     */
    @Override
    public void close() {
        threads.forEach(Thread::interrupt);

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException ignored) {
            }
        }
    }
}
