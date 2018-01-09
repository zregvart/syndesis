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
package io.syndesis.project.converter.visitor;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Queue;

import io.syndesis.core.MavenProperties;
import io.syndesis.dao.manager.DataManager;
import io.syndesis.integration.model.steps.Endpoint;
import io.syndesis.integration.model.steps.Filter;
import io.syndesis.model.connection.Connector;
import io.syndesis.model.filter.ExpressionFilterStep;
import io.syndesis.model.integration.SimpleStep;
import io.syndesis.model.integration.Step;
import io.syndesis.project.converter.ProjectGeneratorProperties;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FilterStepVisitorTest {

    private final FilterStepVisitor filterStepVisitor = new FilterStepVisitor() {
        @Override
        protected String getStepKind() {
            return ExpressionFilterStep.STEP_KIND;
        }
    };

    @Test
    public void shouldMaintainIndex() {
        final Queue<Step> remaining = new ArrayDeque<>();
        remaining.add(new SimpleStep.Builder().stepKind("endpoint").build());
        remaining.add(new SimpleStep.Builder().stepKind("endpoint").build());

        @SuppressWarnings("unchecked")
        final StepVisitorFactory<StepVisitor> factory = mock(StepVisitorFactory.class);
        final StepVisitor visitor = mock(StepVisitor.class);
        when(factory.getStepKind()).thenReturn("endpoint");
        when(factory.create()).thenReturn(visitor);

        final ArgumentCaptor<StepVisitorContext> contexts = ArgumentCaptor.forClass(StepVisitorContext.class);
        when(visitor.visit(contexts.capture())).thenReturn(Collections.singleton(new Endpoint()));

        final StepVisitorFactoryRegistry factoryRegistry = new StepVisitorFactoryRegistry(factory);

        final DataManager dataManager = mock(DataManager.class);
        when(dataManager.fetch(Connector.class, "test")).thenReturn(new Connector.Builder().build());

        final GeneratorContext generatorContext = new GeneratorContext.Builder()//
            .visitorFactoryRegistry(factoryRegistry)//
            .dataManager(dataManager)//
            .generatorProperties(new ProjectGeneratorProperties(new MavenProperties()))//
            .build();

        final StepVisitorContext context = new StepVisitorContext.Builder()//
            .index(1)//
            .step(new ExpressionFilterStep.Builder().build())//
            .remaining(remaining)//
            .generatorContext(generatorContext)//
            .build();

        final Collection<io.syndesis.integration.model.steps.Step> steps = filterStepVisitor.visit(context);

        assertThat(steps).hasSize(1).satisfies(Filter.class::isInstance);
        assertThat((Filter) steps.iterator().next()).satisfies(filter -> {
            assertThat(filter.getSteps()).hasSize(2);
        });

        final List<StepVisitorContext> allContexts = contexts.getAllValues();
        assertThat(allContexts).hasSize(2);
        assertThat(allContexts.get(0).getIndex()).isEqualTo(2);
        assertThat(allContexts.get(1).getIndex()).isEqualTo(3);
    }
}
