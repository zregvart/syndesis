/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.syndesis.common.model.perf;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.profiler.Profiler;

public final class Performance {

    public static final Performance INSTANCE = new Performance();

    private final Map<String, Profiler> profilers = new HashMap<>();

    private Performance() {
        // singleton
    }

    public void clear(final String group) {
        profilers.remove(group);
    }

    public String dump(final String group) {
        final Profiler profiler = profilers.get(group);

        if (profiler == null) {
            return null;
        }

        return profiler.toString();
    }

    public void mark(final String group, final String phase) {
        final Profiler profiler = profilers.computeIfAbsent(group, g -> new Profiler(g));

        profiler.start(phase);
    }

    public void stop(final String group) {
        final Profiler profiler = profilers.get(group);

        if (profiler == null) {
            return;
        }

        profiler.stop();
    }
}
