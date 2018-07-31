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
package io.syndesis.server.logging.jsondb.controller;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.internal.PodOperationsImpl;
import io.fabric8.kubernetes.client.internal.SSLUtils;
import io.fabric8.kubernetes.client.utils.HttpClientUtils;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.matcher.ElementMatchers;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Provides some enriched operations against a KubernetesClient.
 */
public class KubernetesSupport {

    private static final Logger LOG = LoggerFactory.getLogger(KubernetesSupport.class);

    private final KubernetesClient client;
    private final OkHttpClient okHttpClient;

    private X509TrustManager trustManager;

    private static final Class<? extends SocketFactory> SOCKET_FACTORY_DELEGATE_CLASS = new ByteBuddy()
        .subclass(SocketFactory.class)
        .defineField("delegate", SocketFactory.class, Modifier.PUBLIC)
        .method(ElementMatchers.named("createSocket"))
        .intercept(
            Advice.to(SetupKeepAliveAdvice.class)
                .wrap(MethodCall.invokeSelf().onField("delegate").withAllArguments())
        )
        .make()
        .load(KubernetesSupport.class.getClassLoader())
        .getLoaded();

    private static final Field SOCKET_FACTORY_DELEGATE = delegateField(SOCKET_FACTORY_DELEGATE_CLASS);

    private static final Class<? extends SSLSocketFactory> SSL_SOCKET_FACTORY_DELEGATE_CLASS = new ByteBuddy()
        .subclass(SSLSocketFactory.class)
        .defineField("delegate", SSLSocketFactory.class, Modifier.PUBLIC)
        .method(ElementMatchers.named("createSocket"))
        .intercept(
            Advice.to(SetupKeepAliveAdvice.class)
                .wrap(MethodCall.invokeSelf().onField("delegate").withAllArguments())
        )
        .make()
        .load(KubernetesSupport.class.getClassLoader())
        .getLoaded();

    private static final Field SSL_SOCKET_FACTORY_DELEGATE = delegateField(SSL_SOCKET_FACTORY_DELEGATE_CLASS);

    public KubernetesSupport(KubernetesClient client) {
        this.client = client;
        this.okHttpClient = HttpClientUtils.createHttpClient(this.client.getConfiguration());
        final TrustManager[] trustManagers;
        try {
            trustManagers = SSLUtils.trustManagers(this.client.getConfiguration());
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException("Unable to recreate TrustManagers from OKHTTP client configuration", e);
        }
        trustManager = (X509TrustManager) trustManagers[0];
    }


    /*
     * Feeds the controller of the given podName to the callback handler for processing.
     *
     * We do this instead of using the watchLog() feature of the k8s client lib because it really sucks due to:
     *  1. You can't configure the timestamps option or the sinceTime option.  Need to resume log downloads.
     *  2. It seems to need extra threads..
     *  3. It might be hiding some of the http failure conditions.
     *
     */
    protected void watchLog(String podName, Consumer<InputStream> handler, String sinceTime, Executor executor) throws IOException {
        try {
            PodOperationsImpl pod = (PodOperationsImpl) client.pods().withName(podName);
            StringBuilder url = new StringBuilder()
                .append(pod.getResourceUrl().toString())
                .append("/log?pretty=false&follow=true&timestamps=true");
            if (sinceTime != null) {
                url.append("&sinceTime=");
            }
            String podLogUrl = url.toString();

            Thread.currentThread().setName("Logs Controller [running], request: " + podLogUrl);
            Request request = new Request.Builder().url(new URL(podLogUrl)).get().build();

            OkHttpClient clone = okHttpClient.newBuilder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .socketFactory(withKeepalive(okHttpClient.socketFactory()))
                .sslSocketFactory(withKeepalive(okHttpClient.sslSocketFactory()), trustManager)
                .build();

            clone.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    LOG.info("Failure occurred getting  controller for pod: {},", podName, e);
                    handler.accept(null);
                }

                @Override
                public void onResponse(final Call call, final Response response) throws IOException {
                    executor.execute(() -> {
                        Thread.currentThread().setName("Logs Controller [running], streaming: " + podLogUrl);
                        try {
                            if( response.code() == 200 ) {
                                handler.accept(response.body().byteStream());
                            } else {
                                LOG.info("Failure occurred while processing controller for pod: {}, http status: {}, details: {}", podName, response.code(), response.body().string());
                                handler.accept(null);
                            }
                        } catch (IOException e) {
                            LOG.error("Unexpected Error", e);
                        } finally {
                            Thread.currentThread().setName(ActivityTrackingController.IDLE_THREAD_NAME);
                        }
                    });
                }
            });
        } catch (@SuppressWarnings("PMD.AvoidCatchingGenericException") RuntimeException t) {
            throw new IOException("Unexpected Error", t);
        } finally {
            Thread.currentThread().setName(ActivityTrackingController.IDLE_THREAD_NAME);
        }
    }

    private static SocketFactory withKeepalive(final SocketFactory socketFactory) {
        try {
            final SocketFactory delegator = SOCKET_FACTORY_DELEGATE_CLASS.newInstance();
            SOCKET_FACTORY_DELEGATE.set(delegator, socketFactory);
            return delegator;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException("Unable to advise SocketFactory with keep alive advice", e);
        }
    }

    private static SSLSocketFactory withKeepalive(final SSLSocketFactory socketFactory) {
        try {
            final SSLSocketFactory delegator = SSL_SOCKET_FACTORY_DELEGATE_CLASS.newInstance();
            SSL_SOCKET_FACTORY_DELEGATE.set(delegator, socketFactory);
            return delegator;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException("Unable to advise SocketFactory with keep alive advice", e);
        }
    }

    private static Field delegateField(Class<? extends SocketFactory> clazz) {
        try {
            return clazz.getField("delegate");
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("No `delegate` field in the supplied class: " + clazz, e);
        }
    }

}
