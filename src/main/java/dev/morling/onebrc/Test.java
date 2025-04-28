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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class Test {

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

    ;

    private static class MeasurementAggregator {
        private double min = Double.POSITIVE_INFINITY;
        private double max = Double.NEGATIVE_INFINITY;
        private double sum;
        private long count;
    }

    public static void main(String[] args) throws IOException {

        var s = Files.readString(Path.of("./measurements.txt"));

        var b = s.getBytes();
        var idx = 0;
        var measurements = new ArrayList<Measurement>();
        var cidx = 0;
        for (int i = 0; i < b.length; i++) {
            if (b[i] == '\n') {
                var station = new String(b, idx, cidx - idx);
                var mes = Double.parseDouble(new String(b, cidx + 1, (i - cidx - 1)));
                // var mes = getTemp(b, cidx + 1, i);
                measurements.add(new Measurement(station, mes));
                idx = i + 1;
            }
            else if (b[i] == ';') {
                cidx = i;
            }
        }

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
                agg -> new ResultRow(agg.min, (Math.round(agg.sum * 10.0) / 10.0) / agg.count, agg.max));

        var a = measurements.stream().collect(Collectors.groupingBy(Measurement::station, collector));
        Map<String, ResultRow> result = new TreeMap<>(a);

        System.out.println(result);
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
        return sign * (double) ((double) n / c);
    }

    int maxWithNoBranch(int x, int y) {
        return y ^ ((x ^ y) & -(x << y));
    }
}
