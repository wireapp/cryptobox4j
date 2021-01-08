PROMETHEUS_AGENT_VERSION := 0.14.0
PROMETHEUS_AGENT_NAME    := prometheus-java-agent
PROMETHEUS_AGENT_GIT_URL := https://github.com/prometheus/jmx_exporter.git

build/src/$(PROMETHEUS_AGENT_NAME):
	mkdir -p build/src
	cd build/src && \
	git clone $(PROMETHEUS_AGENT_GIT_URL) $(PROMETHEUS_AGENT_NAME) && \
	cd $(PROMETHEUS_AGENT_NAME) && \
	git checkout parent-$(PROMETHEUS_AGENT_VERSION)
