package no.ntnu.ambulanceallocation.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {

    private final static Logger logger = LoggerFactory.getLogger(Utils.class);
    public static final Random random = new Random(10062022);

    public static double randomDouble() {
        return random.nextDouble();
    }

    public static int randomInt(int bound) {
        return random.nextInt(bound);
    }

    public static <T> int randomIndexOf(List<T> list) {
        return random.nextInt(list.size());
    }

    // Pick x random items from a list
    public static <T> List<T> randomChooseN(List<T> list, int numberOfItems) {
        List<T> selection = new ArrayList<>(numberOfItems);
        while (selection.size() < numberOfItems) {
            T element = list.get(randomIndexOf(list));
            if (!selection.contains(element)) {
                selection.add(element);
            }
        }
        return selection;
    }

    public static <T> List<T> filterList(Collection<T> list, Predicate<T> predicate) {
        return list.stream().filter(predicate).toList();
    }

    public static <T, R> List<R> mapList(Collection<T> list, Function<T, R> function) {
        return list.stream().map(function).toList();
    }

    public static double round(double number, double decimalPoints) {
        double multiplier = Math.pow(10, decimalPoints);
        return Math.round(multiplier * number) / multiplier;
    }

    public static void increment(List<Integer> list, int index) {
        Integer value = list.get(index);
        value = value + 1;
        list.set(index, value);
    }

    public static double average(List<Integer> numbers) {
        return numbers.stream().mapToLong(Integer::valueOf).average().orElseThrow();
    }

    public static double median(List<Integer> numbers) {
        Collections.sort(numbers);
        int length = numbers.size();
        int index = length / 2;

        if (length % 2 == 0) {
            long lower = numbers.get(index - 1);
            long upper = numbers.get(index);
            return ((double) lower + (double) upper) / 2.0;
        } else {
            return numbers.get(index);
        }
    }

    public static int medianIndexOf(List<Double> numbers) {
        List<Double> numbersSorted = new ArrayList<>(numbers);
        Collections.sort(numbersSorted);
        Double oddMedian = numbersSorted.get(numbers.size() / 2);
        return numbers.indexOf(oddMedian);
    }

    public static int sign(double number) {
        return (int) Math.signum(number);
    }

    public static double logn(double argument, int base) {
        return Math.log(argument) / Math.log(base);
    }

    public static void timeIt(Runnable func, TimeUnit timeUnit, String text) {
        long startTime = System.nanoTime();
        func.run();
        long elapsedTime = timeUnit.convert((System.nanoTime() - startTime), TimeUnit.NANOSECONDS);
        logger.info(text + elapsedTime);
    }

    public static void timeIt(Runnable func, TimeUnit timeUnit) {
        timeIt(func, timeUnit, "Execution time: ");
    }

    public static void timeIt(Runnable func) {
        timeIt(func, TimeUnit.SECONDS, "Execution time: ");
    }

    public static long timeIt(Runnable func, boolean sysout) {
        long startTime = System.nanoTime();
        func.run();
        return TimeUnit.SECONDS.convert((System.nanoTime() - startTime), TimeUnit.NANOSECONDS);
    }

    public static <T> T timeItAndReturn(Supplier<T> func, String text) {
        long startTime = System.nanoTime();
        T result = func.get();
        logger.info(text
                + TimeUnit.MILLISECONDS.convert((System.nanoTime() - startTime), TimeUnit.NANOSECONDS));
        return result;
    }
}
