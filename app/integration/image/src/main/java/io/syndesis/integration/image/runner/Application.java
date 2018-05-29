package io.syndesis.integration.image.runner;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spring.boot.CamelConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

@SpringBootApplication
public class Application {

    @Component
    public static class StartedRoute extends RouteBuilder {

        private final String name;

        @Autowired
        public StartedRoute(final CamelConfigurationProperties configuration) {
            name = configuration.getName();
        }

        @Override
        public void configure() throws Exception {
            from("timer:go?delay=0&repeatCount=1").to("http4:syndesis-server/api/v1/perf/stop?group=" + name);
        }
    }

    /**
     * A main method to start this application.
     */
    public static void main(final String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
