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
package io.syndesis.integration.runtime.handlers;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import io.syndesis.common.model.action.Action.Pattern;
import io.syndesis.common.model.action.ActionDescriptor.ActionDescriptorStep;
import io.syndesis.common.model.action.ConnectorAction;
import io.syndesis.common.model.action.ConnectorDescriptor;
import io.syndesis.common.model.connection.ConfigurationProperty;
import io.syndesis.common.model.connection.Connection;
import io.syndesis.common.model.connection.Connector;
import io.syndesis.common.model.integration.Step;
import io.syndesis.common.model.integration.StepKind;
import io.syndesis.integration.component.proxy.ComponentProxyComponent;
import io.syndesis.integration.component.proxy.ComponentProxyCustomizer;
import io.syndesis.integration.runtime.capture.OutMessageCaptureProcessor;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileEntry;
import org.mockftpserver.fake.filesystem.FileSystem;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;

import static io.syndesis.integration.runtime.IntegrationTestSupport.newIntegrationRouteBuilder;

import static org.assertj.core.api.Assertions.assertThat;

public class ConnectorStepHandlerPollEnrichIT {

    private static final ConnectorAction FTP_ACTION_FETCH = new ConnectorAction.Builder()
        .id("io.syndesis.ftp:fetch")
        .pattern(Pattern.PollEnrich)
        .descriptor(new ConnectorDescriptor.Builder()
            .addConnectorCustomizer(TestCustomizer.class.getName())
            .addPropertyDefinitionStep(new ActionDescriptorStep.Builder()
                .putProperty("directory", new ConfigurationProperty.Builder()
                    .kind("path")
                    .build())
                .putProperty("fileName", new ConfigurationProperty.Builder()
                    .kind("parameter")
                    .build())
                .build())
            .build())
        .build();

    private static final ConnectorAction FTP_ACTION_FETCH_DYNAMIC = new ConnectorAction.Builder()
        .id("io.syndesis.ftp:fetch-dynamic")
        .pattern(Pattern.PollEnrich)
        .descriptor(new ConnectorDescriptor.Builder()
            .addConnectorCustomizer(TestCustomizer.class.getName())
            .addPropertyDefinitionStep(new ActionDescriptorStep.Builder()
                .putProperty("directory", new ConfigurationProperty.Builder()
                    .kind("path")
                    .build())
                .putProperty("fileName", new ConfigurationProperty.Builder()
                    .kind("proxyParameter")
                    .build())
                .build())
            .build())
        .build();

    private static final Connector FTP_CONNECTOR = new Connector.Builder()
        .id("ftp")
        .componentScheme("ftp")
        .putProperty("host", new ConfigurationProperty.Builder()
            .kind("path")
            .componentProperty(true)
            .build())
        .putProperty("port", new ConfigurationProperty.Builder()
            .kind("path")
            .componentProperty(true)
            .build())
        .putProperty("username", new ConfigurationProperty.Builder()
            .kind("parameter")
            .componentProperty(true)
            .build())
        .putProperty("password", new ConfigurationProperty.Builder()
            .kind("parameter")
            .secret(true)
            .componentProperty(true)
            .build())
        .addAction(FTP_ACTION_FETCH)
        .build();

    private static int port;

    private static FakeFtpServer server;

    private static final ConnectorAction TIMER_ACTION_PERIOD = new ConnectorAction.Builder()
        .id("io.syndesis:timer-action")
        .pattern(Pattern.From)
        .descriptor(new ConnectorDescriptor.Builder()
            .componentScheme("timer")
            .addPropertyDefinitionStep(new ActionDescriptorStep.Builder()
                .putProperty("period", new ConfigurationProperty.Builder()
                    .kind("parameter")
                    .build())
                .build())
            .putConfiguredProperty("timerName", "tick")
            .build())
        .build();

    private static final Connector TIMER_CONNECTOR = new Connector.Builder()
        .id("timer")
        .addAction(TIMER_ACTION_PERIOD)
        .build();

    public static class TestCustomizer implements ComponentProxyCustomizer {

        static boolean afterInvoked;

        static boolean beforeInvoked;

        @Override
        public void customize(final ComponentProxyComponent component, final Map<String, Object> options) {
            component.setBeforeConsumer(exchange -> {
                beforeInvoked = true;
                assertThat(exchange.getProperty(Exchange.TIMER_COUNTER, Integer.class)).isNotZero();
            });
            component.setAfterConsumer(exchange -> {
                afterInvoked = true;
                assertThat(exchange.getIn().getBody(String.class)).matches("Hi .*");
            });
        }
    }

    @Test
    public void shouldSupportPollEnriching() throws Exception {
        final DefaultCamelContext context = new DefaultCamelContext();

        final PropertiesComponent propertiesComponent = new PropertiesComponent();

        final Properties extra = new Properties();
        extra.put("flow-0.ftp-1.password", "password");
        propertiesComponent.setOverrideProperties(extra);

        propertiesComponent.setInitialProperties(extra);
        context.addComponent("properties", propertiesComponent);

        try {
            final RouteBuilder routes = newIntegrationRouteBuilder(
                new Step.Builder()
                    .stepKind(StepKind.endpoint)
                    .action(TIMER_ACTION_PERIOD)
                    .connection(
                        new Connection.Builder()
                            .connector(TIMER_CONNECTOR)
                            .build())
                    .putConfiguredProperty("period", "1000")
                    .build(),
                new Step.Builder()
                    .id("expected")
                    .stepKind(StepKind.endpoint)
                    .action(FTP_ACTION_FETCH)
                    .connection(
                        new Connection.Builder()
                            .connector(FTP_CONNECTOR)
                            .putConfiguredProperty("username", "user")
                            .putConfiguredProperty("password", "/*encrypted*/")
                            .putConfiguredProperty("host", "localhost")
                            .putConfiguredProperty("port", String.valueOf(port))
                            .build())
                    .putConfiguredProperty("directory", "/home/user")
                    .putConfiguredProperty("fileName", "test.txt")
                    .build(),
                new Step.Builder()
                    .stepKind(StepKind.endpoint)
                    .action(new ConnectorAction.Builder()
                        .descriptor(new ConnectorDescriptor.Builder()
                            .componentScheme("mock")
                            .putConfiguredProperty("name", "result")
                            .build())
                        .build())
                    .build());

            context.addRoutes(routes);
            context.start();

            final MockEndpoint result = (MockEndpoint) context.getEndpoints().stream().filter(e -> e instanceof MockEndpoint).findFirst().get();

            MockEndpoint.assertWait(2, TimeUnit.SECONDS, result);

            result.expectedBodiesReceived("Hi there");
            result.allMessages().exchangeProperty(OutMessageCaptureProcessor.CAPTURED_OUT_MESSAGES_MAP).convertTo(String.class).contains("expected");

            MockEndpoint.assertIsSatisfied(context);

            assertThat(TestCustomizer.beforeInvoked).isTrue();
            assertThat(TestCustomizer.afterInvoked).isTrue();
        } finally {
            context.stop();
        }
    }

    @Test
    public void shouldSupportDynamicEndpointParameters() throws Exception {
        final DefaultCamelContext context = new DefaultCamelContext();

        final PropertiesComponent propertiesComponent = new PropertiesComponent();

        final Properties extra = new Properties();
        extra.put("flow-0.ftp-2.password", "password");
        propertiesComponent.setOverrideProperties(extra);

        propertiesComponent.setInitialProperties(extra);
        context.addComponent("properties", propertiesComponent);

        try {
            final RouteBuilder routes = newIntegrationRouteBuilder(
                new Step.Builder()
                    .stepKind(StepKind.endpoint)
                    .action(TIMER_ACTION_PERIOD)
                    .connection(
                        new Connection.Builder()
                            .connector(TIMER_CONNECTOR)
                            .build())
                    .putConfiguredProperty("period", "1000")
                    .build(),
                new Step.Builder()
                    .stepKind(StepKind.headers)
                    .putConfiguredProperty("action", "set")
                    .putConfiguredProperty("CamelFileName", "dynamic.txt")
                    .build(),
                new Step.Builder()
                    .id("expected")
                    .stepKind(StepKind.endpoint)
                    .action(FTP_ACTION_FETCH_DYNAMIC)
                    .connection(
                        new Connection.Builder()
                            .connector(FTP_CONNECTOR)
                            .putConfiguredProperty("username", "user")
                            .putConfiguredProperty("password", "/*encrypted*/")
                            .putConfiguredProperty("host", "localhost")
                            .putConfiguredProperty("port", String.valueOf(port))
                            .build())
                    .putConfiguredProperty("directory", "/home/user")
                    .putConfiguredProperty("fileName", "${header.CamelFileName}")
                    .build(),
                new Step.Builder()
                    .stepKind(StepKind.endpoint)
                    .action(new ConnectorAction.Builder()
                        .descriptor(new ConnectorDescriptor.Builder()
                            .componentScheme("mock")
                            .putConfiguredProperty("name", "result")
                            .build())
                        .build())
                    .build());

            context.addRoutes(routes);
            context.start();

            final MockEndpoint result = (MockEndpoint) context.getEndpoints().stream().filter(e -> e instanceof MockEndpoint).findFirst().get();

            MockEndpoint.assertWait(2, TimeUnit.SECONDS, result);

            result.expectedBodiesReceived("Hi dynamic");
            result.allMessages().exchangeProperty(OutMessageCaptureProcessor.CAPTURED_OUT_MESSAGES_MAP).convertTo(String.class).contains("expected");

            MockEndpoint.assertIsSatisfied(context);

            assertThat(TestCustomizer.beforeInvoked).isTrue();
            assertThat(TestCustomizer.afterInvoked).isTrue();
        } finally {
            context.stop();
        }
    }

    @BeforeAll
    public static void startFtpServer() {
        server = new FakeFtpServer();
        server.setServerControlPort(0);
        server.addUserAccount(new UserAccount("user", "password", "/home/user"));

        final FileSystem files = new UnixFakeFileSystem();
        files.add(new DirectoryEntry("/home/user"));
        files.add(new FileEntry("/home/user/test.txt", "Hi there"));
        files.add(new FileEntry("/home/user/dynamic.txt", "Hi dynamic"));
        server.setFileSystem(files);

        server.start();

        port = server.getServerControlPort();
    }

    @AfterAll
    public static void stopFtpServer() {
        server.stop();
    }
}
