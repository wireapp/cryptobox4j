FROM ubuntu:xenial

ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update \
    && apt-get install openjdk-8-jre-headless -qqy --no-install-recommends \
    && apt-get install libc++abi-dev -qqy --no-install-recommends \
    && apt-get install protobuf-c-compiler -qqy --no-install-recommends \
    && apt-get install libxcomposite-dev -qqy --no-install-recommends \
    && apt-get install libxdamage-dev  -qqy --no-install-recommends \
    && apt-get install libxrender-dev  -qqy --no-install-recommends \
    && apt-get install libevent-dev  -qqy --no-install-recommends \
    && apt-get install libc++-dev  -qqy --no-install-recommends

ENV JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64

RUN mkdir -p /opt/wire/lib
ENV LD_LIBRARY_PATH=/opt/wire/lib

COPY libs/libsodium.so        /opt/wire/lib/libsodium.so
COPY libs/libcryptobox.so     /opt/wire/lib/libcryptobox.so
COPY libs/libcryptobox-jni.so /opt/wire/lib/libcryptobox-jni.so

COPY vendor/UnlimitedJCEPolicyJDK8/local_policy.jar     ${JAVA_HOME}/jre/lib/security/local_policy.jar
COPY vendor/UnlimitedJCEPolicyJDK8/US_export_policy.jar ${JAVA_HOME}/jre/lib/security/US_export_policy.jar
COPY vendor/jmx_prometheus_javaagent/jmx_prometheus_javaagent.jar /opt/wire/lib/jmx_prometheus_javaagent.jar
COPY metrics.yaml  /opt/wire/lib/metrics.yaml
