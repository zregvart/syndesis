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
package io.syndesis.connector.sql;

import io.syndesis.connector.sql.stored.JSONBeanUtil;

import org.apache.camel.CamelContext;
import org.apache.camel.Consumer;
import org.apache.camel.Endpoint;
import org.apache.camel.EndpointConfiguration;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Expression;
import org.apache.camel.PollingConsumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.builder.ExpressionBuilder;
import org.apache.camel.component.connector.DefaultConnectorComponent;
import org.apache.camel.component.connector.SchedulerTimerConnectorEndpoint;
import org.apache.camel.processor.Splitter;

import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Camel SqlConnector connector
 */
public class SqlStartConnectorComponent extends DefaultConnectorComponent {

    final static String COMPONENT_NAME = "sql-start-connector";
    final static String COMPONENT_SCHEME = "sql-start-connector";

    public SqlStartConnectorComponent() {
        super(COMPONENT_NAME, SqlStartConnectorComponent.class.getName());
        registerExtension(new SqlConnectorVerifierExtension(COMPONENT_SCHEME));
        registerExtension(SqlConnectorMetaDataExtension::new);
    }

    // @Override
    // public Endpoint createEndpoint(String uri) throws Exception {
    // // TODO Auto-generated method stub
    // Endpoint endpoint = super.createEndpoint(uri);
    // Producer producer = endpoint.createProducer();
    //
    //
    //
    // return endpoint;
    // }

    public SqlStartConnectorComponent(final String componentScheme) {
        super(COMPONENT_NAME, SqlStartConnectorComponent.class.getName());
    }

    @Override
    public Processor getBeforeProducer() {

        final Processor processor = exchange -> {
            final String body = (String) exchange.getIn().getBody();
            if (body != null) {
                final Properties properties = JSONBeanUtil.parsePropertiesFromJSONBean(body);
                exchange.getIn().setBody(properties);
            }
        };
        return processor;
    }

    @Override
    protected Endpoint createEndpoint(final String uri, final String remaining, final Map<String, Object> parameters) throws Exception {
        final SchedulerTimerConnectorEndpoint endpoint = (SchedulerTimerConnectorEndpoint) super.createEndpoint(uri, remaining, parameters);

        return new Endpoint() {

            @Override
            public void configureProperties(final Map<String, Object> options) {
                endpoint.configureProperties(options);
            }

            @Override
            public Consumer createConsumer(final Processor processor) throws Exception {
                final Expression expression = ExpressionBuilder.bodyExpression(List.class);
                final Splitter splitter = new Splitter(getCamelContext(), expression, processor, null);

                endpoint.setAfterProducer(splitter);

                return endpoint.createConsumer(processor);
            }

            @Override
            public Exchange createExchange() {
                return endpoint.createExchange();
            }

            @Override
            public Exchange createExchange(final Exchange exchange) {
                return endpoint.createExchange(exchange);
            }

            @Override
            public Exchange createExchange(final ExchangePattern pattern) {
                return endpoint.createExchange(pattern);
            }

            @Override
            public PollingConsumer createPollingConsumer() throws Exception {
                return endpoint.createPollingConsumer();
            }

            @Override
            public Producer createProducer() throws Exception {
                return endpoint.createProducer();
            }

            @Override
            public CamelContext getCamelContext() {
                return endpoint.getCamelContext();
            }

            @Override
            public EndpointConfiguration getEndpointConfiguration() {
                return endpoint.getEndpointConfiguration();
            }

            @Override
            public String getEndpointKey() {
                return endpoint.getEndpointKey();
            }

            @Override
            public String getEndpointUri() {
                return endpoint.getEndpointUri();
            }

            @Override
            public boolean isLenientProperties() {
                return endpoint.isLenientProperties();
            }

            @Override
            public boolean isSingleton() {
                return endpoint.isSingleton();
            }

            @Override
            public void setCamelContext(final CamelContext context) {
                endpoint.setCamelContext(context);
            }

            @Override
            public void start() throws Exception {
                endpoint.start();
            }

            @Override
            public void stop() throws Exception {
                endpoint.stop();
            }
        };
    }

}
