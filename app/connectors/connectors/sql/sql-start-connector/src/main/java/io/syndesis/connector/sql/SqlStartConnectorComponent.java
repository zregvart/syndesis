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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.builder.ExpressionBuilder;
import org.apache.camel.builder.ValueBuilder;
import org.apache.camel.component.connector.DefaultConnectorComponent;
import org.apache.camel.processor.ChoiceProcessor;
import org.apache.camel.processor.FilterProcessor;
import org.apache.camel.processor.Pipeline;
import org.apache.camel.processor.Splitter;
import org.apache.camel.processor.aggregate.AggregationStrategy;

import io.syndesis.connector.sql.stored.JSONBeanUtil;

/**
 * Camel SqlConnector connector
 */
public class SqlStartConnectorComponent extends DefaultConnectorComponent {

    final static String COMPONENT_NAME  ="sql-start-connector";
    final static String COMPONENT_SCHEME="sql-start-connector";

    public SqlStartConnectorComponent() {
        super(COMPONENT_NAME, SqlStartConnectorComponent.class.getName());
        registerExtension(new SqlConnectorVerifierExtension(COMPONENT_SCHEME));
        registerExtension(SqlConnectorMetaDataExtension::new);
    }

    public SqlStartConnectorComponent(String componentScheme) {
        super(COMPONENT_NAME, SqlStartConnectorComponent.class.getName());
    }

//    @Override
//    public Endpoint createEndpoint(String uri) throws Exception {
//        // TODO Auto-generated method stub
//        Endpoint endpoint = super.createEndpoint(uri);
//        Producer producer = endpoint.createProducer();
//        
//        
//        
//        return endpoint;
//    }
    
    
    
    @Override
    public Processor getBeforeProducer() {

        final Processor processor = exchange -> {
            final String body = (String) exchange.getIn().getBody();
            if (body!=null) {
                final Properties properties = JSONBeanUtil.parsePropertiesFromJSONBean(body);
                exchange.getIn().setBody(properties);
            }
        };
        return processor;
    }

    @Override
    public Processor getAfterProducer() {
        @SuppressWarnings("unchecked")
        List<String> jsonList = new ArrayList<>();
        final Processor processor = exchange -> {
            
            if (exchange.getIn().getBody(List.class) != null) {
                List<Map> list = exchange.getIn().getBody(List.class);
                for (Map map : list) {
                    //String jsonBean = JSONBeanUtil.toJSONBean(map);
                    List<String> names = exchange.getContext().getComponentNames();
                    Collection endpoints = exchange.getContext().getEndpoints();
                    //exchange.getContext().createProducerTemplate().sendBody("stream:out",jsonBean);
                    //exchange.setProperty(Exchange.ROUTE_STOP, Boolean.TRUE); 
                }
            }
//            } else {
//            if (exchange.getIn().getBody().getClass().equals(Map.class)) {
//                String jsonBean = JSONBeanUtil.toJSONBean(exchange.getIn().getBody(Map.class));
//                exchange.getIn().setBody("JSON");
//                
//                exchange.getContext().createProducerTemplate().sendBody(jsonBean);
//                exchange.setProperty(Exchange.ROUTE_STOP, Boolean.TRUE); 
//            }
            
        };
        
        //FilterProcessor choice = new FilterProcessor(predicate, processor)
            Expression expression = ExpressionBuilder.bodyExpression(List.class);
            Splitter splitter = new Splitter(getCamelContext(), expression, processor, null);
            final Processor pipeline = Pipeline.newInstance(getCamelContext(), splitter);
        
            return pipeline;
        
        
    }

}
