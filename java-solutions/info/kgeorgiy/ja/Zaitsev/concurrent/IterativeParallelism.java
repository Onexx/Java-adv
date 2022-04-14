package info.kgeorgiy.ja.Zaitsev.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ScalarIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.function.Predicate;


/**
 * Implementation of {@link ScalarIP} interface. Provides methods for processing lists in multiple threads.
 *
 * @author Zaitsev Ilya
 */
public class IterativeParallelism implements ScalarIP {
    private final ParallelMapper parallelMapper;

    /**
     * Creates {@link IterativeParallelism} without mapper.
     */
    public IterativeParallelism() {
        parallelMapper = null;
    }

    /**
     * Creates {@link IterativeParallelism} with given {@link ParallelMapper}.
     */
    public IterativeParallelism(ParallelMapper parallelMapper) {
        this.parallelMapper = parallelMapper;
    }

    /**
     * Returns maximum value.
     *
     * @param threads    number or concurrent threads.
     * @param values     values to get maximum of.
     * @param comparator value comparator.
     * @param <T>        value type.
     * @return maximum of given values
     * @throws InterruptedException             if executing thread was interrupted.
     * @throws java.util.NoSuchElementException if no values are given.
     */
    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return process(threads, values, ts -> ts.stream().max(comparator).orElseThrow(NoSuchElementException::new))
                .stream().max(comparator).orElseThrow(NoSuchElementException::new);
    }

    /**
     * Returns minimum value.
     *
     * @param threads    number or concurrent threads.
     * @param values     values to get minimum of.
     * @param comparator value comparator.
     * @param <T>        value type.
     * @return minimum of given values
     * @throws InterruptedException             if executing thread was interrupted.
     * @throws java.util.NoSuchElementException if no values are given.
     */
    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return maximum(threads, values, comparator.reversed());
    }

    /**
     * Returns whether all values satisfies predicate.
     *
     * @param threads   number or concurrent threads.
     * @param values    values to test.
     * @param predicate test predicate.
     * @param <T>       value type.
     * @return whether all values satisfies predicate or {@code true}, if no values are given
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return process(threads, values, ts -> ts.stream().allMatch(predicate)).stream().allMatch(t -> t);
    }

    /**
     * Returns whether any of values satisfies predicate.
     *
     * @param threads   number or concurrent threads.
     * @param values    values to test.
     * @param predicate test predicate.
     * @param <T>       value type.
     * @return whether any value satisfies predicate or {@code false}, if no values are given
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return !all(threads, values, predicate.negate());
    }

    private <T, R> List<R> process(int threads, List<? extends T> values, Function<List<? extends T>, R> function) throws InterruptedException {
        threads = Math.max(Math.min(threads, values.size()), 1);
        List<List<? extends T>> lists = split(threads, values);
        if (parallelMapper != null) {
            return parallelMapper.map(function, lists);
        }
        Thread[] threadsArr = new Thread[threads];
        List<R> results = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            results.add(null);
        }
        for (int i = 0; i < threads; i++) {
            final int ti = i;
            threadsArr[i] = new Thread(() -> results.set(ti, function.apply(lists.get(ti))));
            threadsArr[i].start();
        }
        for (int i = 0; i < threads; i++) {
            threadsArr[i].join();
        }
        return results;
    }

    private <T> List<List<? extends T>> split(int threads, List<? extends T> values) {
        List<List<? extends T>> result = new ArrayList<>();
        int blockSize = values.size() / threads;
        int extra = values.size() % threads;
        int ptr = 0;
        for (int i = 0; i < threads; i++) {
            int curSize = blockSize + (extra > 0 ? 1 : 0);
            result.add(values.subList(ptr, ptr + curSize));
            extra--;
            ptr += curSize;
        }
        return result;
    }
}
