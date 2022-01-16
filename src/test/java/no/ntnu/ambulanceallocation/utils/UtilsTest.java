package no.ntnu.ambulanceallocation.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

public class UtilsTest {

    private final static List<Integer> exampleResponseTimesEven = Arrays.asList(8984, 13937, 1209, 1072, 2190, 2153,
            687, 730, 1012, 625, 2202, 529, 1542, 739, 320, 704, 5851, 1964);
    private final static List<Integer> exampleResponseTimesOdd = Arrays.asList(362, 431, 614, 814, 418, 667, 654,
            628, 368, 422, 223, 431, 539, 512, 470, 591, 595, 834, 1258);

    @Test
    public void medianOfEvenList() {
        double median = Utils.median(exampleResponseTimesEven);
        System.out.println(median);
        assertEquals(1140.50, median);
    }

    @Test
    public void medianOfOddList() {
        double median = Utils.median(exampleResponseTimesOdd);
        assertEquals(539.00, median);
    }

}
