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
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;

public class CalculateAverage_javamak {

    private static final String FILE = "./measurements.txt";

    private record Measurement(String station, double value) {
    }

    private record ResultRow(double min, double mean, double max) {
        public String toString() {
            return round(min) + "/" + round(mean) + "/" + round(max);
        }

        private double round(double value) {
            return Math.round(value * 10.0) / 10.0;
        }
    }

    private static class MeasurementAggregator {
        private double min = Double.POSITIVE_INFINITY;
        private double max = Double.NEGATIVE_INFINITY;
        private double sum;
        private long count;
    }

    private static void spawnWorker() throws IOException {
        ProcessHandle.Info info = ProcessHandle.current().info();
        ArrayList<String> workerCommand = new ArrayList<>();
        info.command().ifPresent(workerCommand::add);
        info.arguments().ifPresent(args -> workerCommand.addAll(Arrays.asList(args)));
        workerCommand.add("--worker");
        new ProcessBuilder().command(workerCommand).inheritIO().redirectOutput(ProcessBuilder.Redirect.PIPE).start().getInputStream().transferTo(System.out);
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        // Thread.sleep(5000);

        if (args.length == 0 || !("--worker".equals(args[0]))) {
            spawnWorker();
            return;
        }

        Collector<Measurement, MeasurementAggregator, ResultRow> collector = Collector.of(MeasurementAggregator::new, (a, m) -> {
            a.min = Math.min(a.min, m.value);
            a.max = Math.max(a.max, m.value);
            a.sum += m.value;
            a.count++;
        }, (agg1, agg2) -> {
            var res = new MeasurementAggregator();
            res.min = Math.min(agg1.min, agg2.min);
            res.max = Math.max(agg1.max, agg2.max);
            res.sum = agg1.sum + agg2.sum;
            res.count = agg1.count + agg2.count;

            return res;
        }, agg -> new ResultRow(agg.min, (Math.round(agg.sum * 10.0) / 10.0) / agg.count, agg.max));

        var path = Paths.get(FILE);

        var a = calcChunks(path).entrySet().parallelStream()
                .flatMap(entry -> getMeasurementFromFile(path, entry)) // read file for each chunk and get the lines
                .collect(groupingBy(Measurement::station, collector));
        Map<String, ResultRow> measurements = new TreeMap<>(a);

        System.out.println(measurements);
    }

    private static Stream<Measurement> getMeasurementFromFile(Path path, Map.Entry<Long, Long> entry) {
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            channel.position(entry.getKey());
            ByteBuffer buffer = ByteBuffer.allocate((int) (entry.getValue() - entry.getKey() + 1));
            channel.read(buffer);

            var b = buffer.array();
            var idx = 0;
            var res = new ArrayList<Measurement>();
            var cidx = 0;
            for (int i = 0; i < b.length; i++) {

                if (b[i] == '\n') {
                    var station = new String(b, idx, cidx - idx);
                    var mes = getTemp(b, cidx + 1, i);

                    res.add(new Measurement(station, mes));
                    idx = i + 1;
                }
                else if (b[i] == ';') {
                    cidx = i;
                }
            }
            return res.stream();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static double getTemp(byte[] bytes, int sidx, int eidx) {
        int n = 0;
        int c = 0;
        int sign = 1;
        for (int i = sidx; i < eidx; i++) {
            if (bytes[i] == '-') {
                sign = -1;
                continue;
            }
            else if (bytes[i] == '.') {
                c = 1;
                continue;
            }
            n = (n * 10) + (bytes[i] - '0');
            c *= 10;
        }
        return sign * ((double) n / c);
    }

    private static Map<Long, Long> calcChunks(Path path) throws IOException {
        long startPos = 0;
        Map<Long, Long> retMap = new HashMap<>();
        while (true) {
            long endPos = calculateEndPosition(path, startPos, 1000 * 2000);
            if (endPos == -1) {
                break;
            }
            long finalStartPos = startPos;
            retMap.put(finalStartPos, endPos);
            startPos = endPos + 1;
        }
        return retMap;
    }

    private static long calculateEndPosition(Path path, long startPos, long chunkSize) throws IOException {
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            if (startPos >= channel.size()) {
                return -1;
            }

            long currentPos = startPos + chunkSize;
            if (currentPos >= channel.size()) {
                currentPos = channel.size() - 1;
            }

            channel.position(currentPos);
            ByteBuffer buffer = ByteBuffer.allocate(100);
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
