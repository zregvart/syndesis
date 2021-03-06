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

package io.syndesis.test.integration.source;

import java.util.List;
import java.util.Map;

import io.syndesis.common.model.integration.Integration;
import io.syndesis.common.model.openapi.OpenApi;
import io.syndesis.test.integration.customizer.IntegrationCustomizer;

public class CustomizedIntegrationSource implements IntegrationSource {

    private final IntegrationSource delegate;
    private final List<IntegrationCustomizer> customizers;

    public CustomizedIntegrationSource(IntegrationSource delegate, List<IntegrationCustomizer> customizers) {
        this.delegate = delegate;
        this.customizers = customizers;
    }

    @Override
    public Integration get() {
        Integration integration = delegate.get();
        for (IntegrationCustomizer customizer : customizers) {
            integration = customizer.apply(integration);
        }

        return integration;
    }

    @Override
    public Map<String, OpenApi> getOpenApis() {
        return delegate.getOpenApis();
    }
}
