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

import java.util.ArrayList;

public class Test {

    private record Measurement(String station, double value) {
        private Measurement(String[] parts) {
            this(parts[0], Double.parseDouble(parts[1]));
        }
    }

    public static void main(String[] args) {
        var s = """
                Starorybnoye;72.7666
                Sagastyr;73.3779
                Zemlya Bunge;74.8983
                Agapa;71.4504
                Tukchi;57.3670
                Numto;63.6667
                Nord;81.7166
                Timmiarmiut;62.5333
                San Rafael;-16.7795
                Nordvik;74.0165
                """;

        var b = s.getBytes();
        var idx = 0;
        var res = new ArrayList<Measurement>();
        var cidx = 0;
        for (int i = 0; i < b.length; i++) {
            if (b[i] == '\n') {
                var station = new String(b, idx, cidx - idx);
                var mes = Double.parseDouble(new String(b, cidx + 1, (i - cidx - 1)));
                res.add(new Measurement(station, mes));
                idx = i + 1;
            }
            else if (b[i] == ';') {
                cidx = i;
            }
        }
        System.out.println(res.size());
        System.out.println(res);

    }
}
