#!/bin/sh
#
#  Copyright 2023 The original authors
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#

JAVA_OPTS="-Xmx25G -Xms15G -XX:+UseParallelGC"
java $JAVA_OPTS --class-path target/average-1.0.0-SNAPSHOT.jar dev.morling.onebrc.CalculateAverage_javamak

if [ -f target/CalculateAverage_javamak_image ]; then
    #echo "Picking up existing native image 'target/CalculateAverage_javamak_image', delete the file to select JVM mode." 1>&2
    target/CalculateAverage_javamak_image
else
    JAVA_OPTS="-Xmx25G -Xms15G -XX:+UseParallelGC --enable-preview"
#    echo "Chosing to run the app in JVM mode as no native image was found, use prepare_javamak.sh to generate." 1>&2
    java $JAVA_OPTS --class-path target/average-1.0.0-SNAPSHOT.jar dev.morling.onebrc.CalculateAverage_javamak
fi


#sdk use java 24.0.1-graalce
#sudo sysctl kernel.perf_event_paranoid=-1
#perf stat -e branches,branch-misses,cache-references,cache-misses,cycles,instructions,idle-cycles-backend,idle-cycles-frontend,task-clock -- java $JAVA_OPTS --class-path target/average-1.0.0-SNAPSHOT.jar dev.morling.onebrc.CalculateAverage_javamak
#time ./calculate_average_javamak.sh
