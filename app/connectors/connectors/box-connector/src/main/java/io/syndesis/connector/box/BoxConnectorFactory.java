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
package io.syndesis.connector.box;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Component;
import org.apache.camel.component.box.BoxComponent;
import org.apache.camel.component.box.BoxConfiguration;
import org.apache.camel.component.extension.ComponentExtension;
import org.apache.camel.util.IntrospectionSupport;
import org.apache.camel.util.function.Predicates;

import io.syndesis.integration.component.proxy.ComponentDefinition;
import io.syndesis.integration.component.proxy.ComponentProxyComponent;
import io.syndesis.integration.component.proxy.ComponentProxyFactory;

public class BoxConnectorFactory implements ComponentProxyFactory {

    @Override
    public ComponentProxyComponent newInstance(String componentId, String componentScheme) {
        return new BoxProxyComponent(componentId, componentScheme);
    }

    private static class BoxProxyComponent extends ComponentProxyComponent {

        private String userName;
        private String userPassword;
        private String clientId;
        private String clientSecret;

        public BoxProxyComponent(String componentId, String componentScheme) {
            super(componentId, componentScheme);
            // support connection verification
            registerExtension(this::getComponentVerifier);
        }

        private ComponentExtension getComponentVerifier() {
            return new BoxVerifierExtension(getComponentId(), getCamelContext());
        }

        @Override
        public void setOptions(Map<String, Object> options) {
            // connection parameters
            System.out.println("options:::::::::::::::::::::::;" + options);
            userName = (String) options.get("userName");
            userPassword = (String) options.get("userPassword");
            clientId = (String) options.get("clientId");
            clientSecret = (String) options.get("clientSecret");

            /*
             * BoxConfiguration boxConfiguration = new BoxConfiguration();
             * boxConfiguration.setUserName(userName);
             * boxConfiguration.setUserPassword(userPassword);
             * boxConfiguration.setClientId(clientId);
             * boxConfiguration.setClientSecret(clientSecret);
             * boxConfiguration.setAuthenticationType(BoxConfiguration.
             * STANDARD_AUTHENTICATION);
             */

            System.out.println("component proxy class-------------------------------" + this.getClass());
            super.setOptions(options);
        }

        /*
         * @Override protected Endpoint createEndpoint(String uri, String remaining,
         * Map<String, Object> parameters) throws Exception {
         * System.out.println("233333333333333333333333333" + uri + remaining); return
         * super.createEndpoint(uri, remaining, parameters); }
         */

        /*
         * protected Endpoint createEndpoint(String uri, String methodName, BoxApiName
         * apiName, BoxConfiguration endpointConfiguration) throws Exception {
         * 
         * System.out.println("In create endpoint:::::::::::::::::::::::;" + uri +
         * methodName + apiName); System.out.println("233333333333333333333333333" + uri
         * + methodName); BoxComponent delegate = (BoxComponent)
         * getCamelContext().getComponent(getComponentScheme());
         * 
         * BoxEndpoint endpoint = new BoxEndpoint(uri, delegate, apiName, methodName,
         * endpointConfiguration); if (methodName.equals("upload")) {
         * endpoint.setInBody("content"); } else if (methodName.equals("uploadVersion"))
         * { endpoint.setInBody("fileContent"); } else if (methodName.equals("download")
         * || methodName.equals("downloadPreviousFileVersion")) {
         * ((BoxFilesManagerEndpointConfiguration) (endpoint.getConfiguration()))
         * .setOutput(new java.io.ByteArrayOutputStream()); }
         * 
         * return endpoint; }
         */

        @SuppressWarnings("PMD.SignatureDeclareThrowsException")
        protected void configureDelegateComponent(ComponentDefinition definition, Component component,
                Map<String, Object> options) throws Exception {
            final CamelContext context = getCamelContext();
            final List<Map.Entry<String, Object>> entries = new ArrayList<>();

            // Get the list of options from the connector catalog that
            // are configured to target the endpoint
            final Collection<String> endpointOptions = definition.getEndpointProperties().keySet();

            // Check if any of the option applies to the component, if not
            // there's no need to create a dedicated component.
            options.entrySet().stream().filter(e -> !endpointOptions.contains(e.getKey())).forEach(entries::add);

            // Options set on a step are strings so if any of the options is
            // not a string, is should have been added by a customizer so try to
            // bind them to the component first.
            options.entrySet().stream().filter(e -> e.getValue() != null)
                    .filter(Predicates.negate(e -> e.getValue() instanceof String)).forEach(entries::add);

            if (!entries.isEmpty()) {
                component.setCamelContext(context);

                for (Map.Entry<String, Object> entry : entries) {
                    String key = entry.getKey();
                    Object val = entry.getValue();

                    if (val instanceof String) {
                        val = super.getCamelContext().resolvePropertyPlaceholders((String) val);
                    }

                    if (IntrospectionSupport.setProperty(context, component, key, val)) {
                        options.remove(key);
                    }
                }
            }

            userName = (String) options.get("userName");
            userPassword = (String) options.get("userPassword");
            clientId = (String) options.get("clientId");
            clientSecret = (String) options.get("clientSecret");

            BoxConfiguration boxConfiguration = new BoxConfiguration();
            boxConfiguration.setUserName(userName);
            boxConfiguration.setUserPassword(userPassword);
            boxConfiguration.setClientId(clientId);
            boxConfiguration.setClientSecret(clientSecret);
            boxConfiguration.setAuthenticationType(BoxConfiguration.STANDARD_AUTHENTICATION);

            System.out.println("22222222222222222222222222222222");
            ((BoxComponent) (component)).setConfiguration(boxConfiguration);

        }

    }

    /*
     * @Override protected void doStart() throws Exception {
     * System.out.println("in dostartttttttttttttttttttttttttttttttttttttttt"); try
     * { BoxConfiguration boxConfiguration = new BoxConfiguration();
     * boxConfiguration.setUserName(userName);
     * boxConfiguration.setUserPassword(userPassword);
     * boxConfiguration.setClientId(clientId);
     * boxConfiguration.setClientSecret(clientSecret);
     * boxConfiguration.setAuthenticationType(BoxConfiguration.
     * STANDARD_AUTHENTICATION);
     * 
     * BoxComponent delegate = (BoxComponent)
     * getCamelContext().getComponent(getComponentScheme());
     * delegate.setConfiguration(boxConfiguration);
     * 
     * Box boxComponent = new Box(getCamelContext(), boxConfiguration);
     * boxComponent.setConfiguration(boxConfiguration);
     * getCamelContext().addComponent("box", boxComponent);
     * 
     * } catch (Exception e) { // TODO Auto-generated catch block
     * System.out.println("eeeeeeeeeeeeeeeeeeeeeeeeeeeeevvvv" + e); }
     * 
     * super.doStart(); } }
     * 
     * private static class Box extends BoxComponent { BoxConfiguration config =
     * null;
     * 
     * public Box(CamelContext camelContext, BoxConfiguration boxConfiguration) {
     * super(camelContext); config = boxConfiguration; }
     * 
     * @Override protected void doStart() throws Exception {
     * System.out.println("coollllllllllllllllllllllllllllllllllllllllllllllll"); if
     * (configuration == null) { setConfiguration(config); } super.doStart();
     * 
     * }
     * 
     * @Override protected Endpoint createEndpoint(String uri, String methodName,
     * BoxApiName apiName, BoxConfiguration endpointConfiguration) {
     * endpointConfiguration.setApiName(apiName);
     * endpointConfiguration.setMethodName(methodName);
     * 
     * BoxEndpoint endpoint = new BoxEndpoint(uri, this, apiName, methodName,
     * endpointConfiguration); if (methodName.equals("upload")) {
     * endpoint.setInBody("content"); } else if (methodName.equals("uploadVersion"))
     * { endpoint.setInBody("fileContent"); } else if (methodName.equals("download")
     * || methodName.equals("downloadPreviousFileVersion")) {
     * ((BoxFilesManagerEndpointConfiguration) (endpoint.getConfiguration()))
     * .setOutput(new java.io.ByteArrayOutputStream()); } return endpoint; }
     * 
     * }
     */
}
