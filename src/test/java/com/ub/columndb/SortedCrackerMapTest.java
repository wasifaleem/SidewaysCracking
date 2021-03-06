package com.ub.columndb;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import repeat.Repeat;
import repeat.RepeatRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static bench.RangeUtils.data;
import static bench.RangeUtils.randomRange;
import static org.assertj.core.api.Assertions.assertThat;

public class SortedCrackerMapTest {
    private static final int N = 1000;

    private CrackerMap<Integer, Integer> crackerMap;

    @Rule
    public RepeatRule repeatRule = new RepeatRule();

    @Before
    public void setUp() throws Exception {
        List<Integer> h = data(N);
        Collections.sort(h);
        List<Integer> t = new ArrayList<>(Collections.nCopies(N, 0));

        crackerMap = new CrackerMap<>(h, t, new CrackerTape<>(), true); // NOTE: sorted!
    }

    @Test
    @Repeat(100)
    public void scan() throws Exception {
        for (int k = 0; k < N / 2; k++) {
            int[] range = randomRange(N);
            int low = range[0], high = range[1];

            assertThat(crackerMap.scan(low, high))
                    .extracting(t -> t.head)
                    .as("%d <= h <= %d", low, high)
                    .allMatch(h -> h >= low && h <= high);
        }
    }
}
