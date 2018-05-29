FROM fabric8/s2i-java:2.2.0

ENV AB_JOLOKIA_HTTPS="true"

ENV JAVA_APP_DIR=/deployments

USER 0

RUN yum install -y nfs-utils && yum clean all && rm -rf /var/cache/yum

USER 1000

COPY integration-image*.jar /deployments/
COPY prometheus-config.yml /tmp/src/

EXPOSE 8080 8778 9779
