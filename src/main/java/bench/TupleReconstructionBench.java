package bench;


import com.ub.columndb.ColumnDB;
import com.ub.columndb.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static bench.CrackerMapBenchmarks.buildRanges;
import static bench.CrackerMapBenchmarks.time;
import static bench.RangeUtils.data;

public class TupleReconstructionBench {
    private static final Logger LOG = LoggerFactory.getLogger(TupleReconstructionBench.class);

    private static final char SEPARATOR = ',';

    private static final String SELECTION_COL = "A0";
    private static final int N_TUPLE_RECONSTRUCTIONS = 9;


    public static void main(String[] args) throws IOException {
        int warmUpSize = 1000;
        benchmark(warmUpSize, warmUpSize, 1);

        int size = 1_000_000;

        long[][] responseTimes = benchmark(size, 1000, N_TUPLE_RECONSTRUCTIONS);

        output(responseTimes, N_TUPLE_RECONSTRUCTIONS + ".csv");
    }

    static long[][] benchmark(int size, int querySequenceSize, int nProj) {
        int[][] queryRanges = buildRanges(size, querySequenceSize, 0.2f);
        long[] queryResponseTimes = new long[querySequenceSize];
        long[] sortedQueryResponseTimes = new long[querySequenceSize];

        String[] projectionCols = projectionCols(nProj);
        // TODO: sleep, log range, random variance, millis, first jump, sort should flat-line.
        List<Integer> tail = Arrays.asList(new Integer[size]);

        ColumnDB db = new ColumnDB();
        List<Integer> head = data(size);
        db.addColumn(SELECTION_COL, head);
        for (String projectionCol : projectionCols) {
            db.addColumn(projectionCol, tail);
        }
        query(db, queryRanges, projectionCols, queryResponseTimes);


        ColumnDB sortedDB = new ColumnDB();
        List<Integer> sortedHead = new ArrayList<>(head);
        long sortingTime = time(() -> sortedHead.sort(Integer::compareTo));
        LOG.info("PreSorting time: {}", sortingTime);
        sortedDB.addColumn(SELECTION_COL, sortedHead, true);   // <-  SORTED!
        for (String projectionCol : projectionCols) {
            sortedDB.addColumn(projectionCol, tail);
        }
        query(sortedDB, queryRanges, projectionCols, sortedQueryResponseTimes);
        sortedQueryResponseTimes[0] += sortingTime; // add time for pre-sorting

        return new long[][]{queryResponseTimes, sortedQueryResponseTimes};
    }

    private static String[] projectionCols(int nProj) {
        String[] cols = new String[nProj];
        for (int i = 0; i < cols.length; i++) {
            cols[i] = "A" + (i + 1);
        }
        return cols;
    }

    static void query(ColumnDB columnDB, int[][] queryRanges, String[] projectionCols, long[] queryResponseTimes) {
        for (int i = 0; i < queryRanges.length; i++) {
            recordScanTime(columnDB, queryRanges, projectionCols, queryResponseTimes, i);
        }
    }

    static List<List<Tuple<Integer, Integer>>> recordScanTime(ColumnDB columnDB, int[][] queryRanges, String[] projectionCols, long[] queryResponseTimes, int i) {
        return time(() -> columnDB.scan(SELECTION_COL, queryRanges[i][0], queryRanges[i][1], projectionCols), queryResponseTimes, i);
    }

    private static void output(long[][] responseTimes, String file) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(file), StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
            writer.append("QuerySequence").append(SEPARATOR).append("ResponseTime").append(SEPARATOR).append("Type");
            writer.newLine();
            long[] cracked = responseTimes[0];
            long[] sorted = responseTimes[1];
            for (int j = 0, i = 1; j < cracked.length; j++, i++) {
                writer.append(String.valueOf(i));
                writer.append(SEPARATOR);
                writer.append(String.valueOf(cracked[j]));
                writer.append(SEPARATOR);
                writer.append("CRACKED");
                writer.newLine();

                writer.append(String.valueOf(i));
                writer.append(SEPARATOR);
                writer.append(String.valueOf(sorted[j]));
                writer.append(SEPARATOR);
                writer.append("SORTED");
                writer.newLine();
            }
        }
    }
}
