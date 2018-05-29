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
package io.syndesis.server.endpoint.v1.handler.perf;

import java.util.Optional;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.syndesis.common.model.perf.Performance;

import org.springframework.stereotype.Component;

@Path("/perf")
@Api("performance")
@Component
public class PerfHandler {

    @GET
    @Path("/clear")
    @ApiOperation("Clears the profiler record")
    public void clear(@QueryParam("group") final String group) {
        Performance.INSTANCE.clear(group);
    }

    @GET
    @Path("/dump")
    @Produces("text/plain")
    @ApiOperation("Stops the performance tracker and dumps the results")
    public String dump(@QueryParam("group") final String group) {
        return Optional.ofNullable(Performance.INSTANCE.dump(group)).orElse("nada");
    }

    @GET
    @Path("/record")
    @ApiOperation("Records a phase in the performance tracker")
    public void record(@QueryParam("group") final String group, @QueryParam("phase") final String phase) {
        Performance.INSTANCE.mark(group, phase);
    }

    @GET
    @Path("/stop")
    @ApiOperation("Stops recording")
    public void stop(@QueryParam("group") final String group) {
        Performance.INSTANCE.stop(group);
    }
}
