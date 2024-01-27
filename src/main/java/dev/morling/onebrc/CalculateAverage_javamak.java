/*
 *  Copyright 2023 The original authors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package dev.morling.onebrc;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;

public class CalculateAverage_javamak {

    private static final String FILE = "./measurements.txt";

    private static record Measurement(String station, double value) {
        private Measurement(String[] parts) {
            this(parts[0], Double.parseDouble(parts[1]));
        }
    }

    private static record ResultRow(double min, double mean, double max) {
        public String toString() {
            return STR."\{round(min)}/\{round(mean)}/\{round(max)}";
        }

        private double round(double value) {
            return Math.round(value * 10.0) / 10.0;
        }
    }

    ;

    private static class MeasurementAggregator {
        private double min = Double.POSITIVE_INFINITY;
        private double max = Double.NEGATIVE_INFINITY;
        private double sum;
        private long count;
    }

    public static void main(String[] args) throws IOException {

        Collector<Measurement, MeasurementAggregator, ResultRow> collector = Collector.of(
                MeasurementAggregator::new,
                (a, m) -> {
                    a.min = Math.min(a.min, m.value);
                    a.max = Math.max(a.max, m.value);
                    a.sum += m.value;
                    a.count++;
                },
                (agg1, agg2) -> {
                    var res = new MeasurementAggregator();
                    res.min = Math.min(agg1.min, agg2.min);
                    res.max = Math.max(agg1.max, agg2.max);
                    res.sum = agg1.sum + agg2.sum;
                    res.count = agg1.count + agg2.count;

                    return res;
                },
                agg -> new ResultRow(agg.min, agg.sum / agg.count, agg.max));

        var path = Paths.get(FILE);

        var a = calcChunks(path).entrySet().parallelStream()
                .flatMap(entry -> getLinesFromFile(path, entry)) // read file for each chunk and get the lines
                .map(l -> new Measurement(breakBySemiColon(l)))// convert each line to measurement object
                .collect(groupingBy(Measurement::station, collector));
        Map<String, ResultRow> measurements = new TreeMap<>(a);

        System.out.println(measurements);
    }

    static String[] breakBySemiColon(String str) {
        var i = str.indexOf(";");
        return new String[]{ str.substring(0, i), str.substring(i + 1) };
    }

    private static Stream<String> getLinesFromFile(Path path, Map.Entry<Long, Long> entry) {
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            channel.position(entry.getKey());
            ByteBuffer buffer = ByteBuffer.allocate((int) (entry.getValue() - entry.getKey() + 1));
            channel.read(buffer);

            List<String> lst = new ArrayList<>(10000);

            var arr = buffer.array();
            int startIndex = 0;
            for (int i = 0; i < arr.length; i++) {
                if (arr[i] == '\n') {
                    lst.add(new String(Arrays.copyOfRange(arr, startIndex, i)));
                    startIndex = i + 1;
                }
            }
            return lst.stream();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<Long, Long> calcChunks(Path path) throws IOException {
        long startPos = 0;
        Map<Long, Long> retMap = new HashMap<>();
        while (true) {
            long endPos = calculateEndPosition(path, startPos);
            if (endPos == -1) {
                break;
            }
            long finalStartPos = startPos;
            retMap.put(finalStartPos, endPos);
            startPos = endPos + 1;
        }
        return retMap;
    }

    private static long calculateEndPosition(Path path, long startPos) throws IOException {
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            if (startPos >= channel.size()) {
                return -1;
            }

            long currentPos = startPos + (long) 100000;
            if (currentPos >= channel.size()) {
                currentPos = channel.size() - 1;
            }

            channel.position(currentPos);
            ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
            int readBytes = channel.read(buffer);

            if (readBytes > 0) {
                for (int i = 0; i < readBytes; i++) {
                    if (buffer.get(i) == '\n') {
                        break;
                    }
                    currentPos++;
                }
            }

            return currentPos;
        }
    }
}
