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

import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.component.box.BoxConfiguration;
import org.apache.camel.component.box.internal.BoxConnectionHelper;
import org.apache.camel.component.extension.verifier.DefaultComponentVerifierExtension;
import org.apache.camel.component.extension.verifier.ResultBuilder;
import org.apache.camel.component.extension.verifier.ResultErrorBuilder;
import org.apache.camel.component.extension.verifier.ResultErrorHelper;

import com.box.sdk.BoxAPIConnection;

public class BoxVerifierExtension extends DefaultComponentVerifierExtension {

    protected BoxVerifierExtension(String defaultScheme, CamelContext context) {
        super(defaultScheme, context);
    }

    // *********************************
    // Parameters validation
    //
    protected Result verifyParameters(Map<String, Object> parameters) {
        ResultBuilder builder = ResultBuilder.withStatusAndScope(Result.Status.OK, Scope.PARAMETERS)
                .error(ResultErrorHelper.requiresOption("userName", parameters))
                .error(ResultErrorHelper.requiresOption("userPassword", parameters))
                .error(ResultErrorHelper.requiresOption("clientId", parameters))
                .error(ResultErrorHelper.requiresOption("clientSecret", parameters));
        if (builder.build().getErrors().isEmpty()) {
            verifyCredentials(builder, parameters);
        }
        return builder.build();
    }

    // *********************************
    // Connectivity validation
    // *********************************
    @Override
    protected Result verifyConnectivity(Map<String, Object> parameters) {
        return ResultBuilder.withStatusAndScope(Result.Status.OK, Scope.CONNECTIVITY)
                .error(parameters, this::verifyCredentials).build();
    }

    private void verifyCredentials(ResultBuilder builder, Map<String, Object> parameters) {

        String userName = (String) parameters.get("userName");
        String userPassword = (String) parameters.get("userPassword");
        String clientId = (String) parameters.get("clientId");
        String clientSecret = (String) parameters.get("clientSecret");

        BoxConfiguration boxConfiguration = new BoxConfiguration();
        boxConfiguration.setUserName(userName);
        boxConfiguration.setUserPassword(userPassword);
        boxConfiguration.setClientId(clientId);
        boxConfiguration.setClientSecret(clientSecret);
        boxConfiguration.setAuthenticationType(BoxConfiguration.STANDARD_AUTHENTICATION);

        try {
            BoxAPIConnection connection = BoxConnectionHelper.createStandardAuthenticatedConnection(boxConfiguration);
            connection = null;
        } catch (Exception e) {

            builder.error(ResultErrorBuilder
                    .withCodeAndDescription(VerificationError.StandardCode.AUTHENTICATION, e.getMessage())
                    .parameterKey("userName").parameterKey("userPassword").parameterKey("clientId")
                    .parameterKey("clientSecret").build());
            System.out.println("authentication failed");
            e.printStackTrace();
        }

        /*
         * 
         * final String host = (String) parameters.get("host"); final int port =
         * Integer.parseInt((String) parameters.get("port")); final String userName =
         * (parameters.get("username") == null) ? "anonymous" : (String)
         * parameters.get("username"); final String password =
         * (parameters.get("password") == null) || "anonymous".equals(userName) ? "" :
         * (String) parameters.get("password");
         * 
         * int reply; FTPClient ftp = new FTPClient();
         * 
         * String illegalParametersMessage = "Unable to connect to the FTP server";
         * boolean hasValidParameters = false;
         * 
         * try { ftp.connect(host, port); reply = ftp.getReplyCode(); hasValidParameters
         * = FTPReply.isPositiveCompletion(reply); } catch (IOException e) {
         * illegalParametersMessage = e.getMessage(); }
         * 
         * if (!hasValidParameters) { builder.error(
         * ResultErrorBuilder.withCodeAndDescription(VerificationError.StandardCode.
         * ILLEGAL_PARAMETER_VALUE,
         * illegalParametersMessage).parameterKey("host").parameterKey("port").build());
         * 
         * } else {
         * 
         * boolean isAuthenticated = false; String authentionErrorMessage =
         * "Authentication failed";
         * 
         * try { isAuthenticated = ftp.login(userName, password); } catch (IOException
         * ioe) { authentionErrorMessage = ioe.getMessage(); } if (!isAuthenticated) {
         * 
         * builder.error(ResultErrorBuilder
         * .withCodeAndDescription(VerificationError.StandardCode.AUTHENTICATION,
         * authentionErrorMessage)
         * .parameterKey("username").parameterKey("password").build());
         * 
         * } else { try { ftp.logout(); ftp.disconnect(); } catch (IOException ignored)
         * { // ignore }
         * 
         * } }
         * 
         */}

}
