/*
 * Copyright (C) 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.syndesis.server.endpoint.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.syndesis.common.model.ListResult;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class PaginationFilterTest {

    private static final List<Integer> ints = Arrays.asList(1, 2, 3, 4, 5);

    static List<Parameter> parameters() {
        return Arrays.asList(
            new Parameter(1, 1, ints, Arrays.asList(1), null),
            new Parameter(2, 1, ints, Arrays.asList(2), null),
            new Parameter(2, 3, ints, Arrays.asList(4, 5), null),
            new Parameter(1, 5, ints, Arrays.asList(1, 2, 3, 4, 5), null),
            new Parameter(2, 5, ints, Collections.emptyList(), null),
            new Parameter(-1, 1, ints, null, IllegalArgumentException.class),
            new Parameter(1, -1, ints, null, IllegalArgumentException.class)
        );
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void apply(final Parameter parameter) throws Exception {
        try {
            ListResult<Integer> filtered = new PaginationFilter<Integer>(new PaginationOptions() {
                @Override
                public int getPage() {
                    return parameter.page;
                }

                @Override
                public int getPerPage() {
                    return parameter.perPage;
                }
            }).apply(new ListResult.Builder<Integer>().items(parameter.inputList).totalCount(parameter.inputList.size()).build());

            assertEquals(parameter.outputList, filtered.getItems());
            assertEquals(parameter.inputList.size(), filtered.getTotalCount());
        } catch (Exception e) {
            if (parameter.expectedException == null) {
                throw e;
            }
            assertEquals(parameter.expectedException, e.getClass());
            return;
        }
        if (parameter.expectedException != null) {
            fail("Expected exception " + parameter.expectedException);
        }
    }

    private static class Parameter {
        int page;
        int perPage;
        List<Integer> inputList;
        List<Integer> outputList;
        Class<? extends Exception> expectedException;

        Parameter(int page, int perPage, List<Integer> inputList, List<Integer> outputList, Class<? extends Exception> expectedException) {
            this.page = page;
            this.perPage = perPage;
            this.inputList = inputList;
            this.outputList = outputList;
            this.expectedException = expectedException;
        }
    }

}
