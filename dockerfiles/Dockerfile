FROM openjdk:11-jdk as cryptobox

# disable prompts from the txdata
ENV DEBIAN_FRONTEND=noninteractive
# update dependencies in the base image
RUN apt-get update && apt-get upgrade -y
# install build tools
RUN apt-get install -qqy --no-install-recommends \
    	git \
    	wget \
    	build-essential \
    	gcc \
    	software-properties-common \
    	unzip \
    	clang \
    	vim \
    	pkg-config \
    	strace \
    	less \
    	sudo \
    	curl \
    	maven

# install rust
RUN curl https://sh.rustup.rs -sSf | bash -s -- -y
ENV PATH="/root/.cargo/bin:${PATH}"

# prepare final builds
WORKDIR /wire/cryptobox
COPY Makefile .

##### build Wire cryptobox
COPY mk ./mk
RUN make cryptobox

##### build native Cryptobox-Jni
RUN mkdir src
COPY src/cryptobox-jni.c ./src/
RUN make compile-native

##### build java wrapper and prometheus java agent around cryptobox
COPY src/main ./src/main
COPY src/test ./src/test
COPY pom.xml .
# build and package everything to /wire/cryptobox/dist
RUN make dist

ENV LD_LIBRARY_PATH=/wire/cryptobox/dist/lib

FROM openjdk:11-jre-slim as runtime

RUN mkdir -p /opt/wire/lib
# make Java to take this as java.library.path
ENV LD_LIBRARY_PATH=/opt/wire/lib

# copy cryptobox libraries
COPY --from=cryptobox /wire/cryptobox/dist/lib /opt/wire/lib
# copy prometheus agent
COPY --from=cryptobox /wire/cryptobox/dist/jar/prometheus-agent.jar /opt/wire/lib/
# copy metrics seettings
COPY dockerfiles/metrics.yaml /opt/wire/lib/metrics.yaml

# enable crypto https://stackoverflow.com/a/42163925/7169288
# it should be enabled by default, we're doing this just to be sure
RUN sed -i 's/^#crypto.policy=unlimited/crypto.policy=unlimited/' $JAVA_HOME/conf/security/java.security
