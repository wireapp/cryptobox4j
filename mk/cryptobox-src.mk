CRYPTOBOX_VERSION := v1.0.0
CRYPTOBOX         := cryptobox-$(CRYPTOBOX_VERSION)
CRYPTOBOX_GIT_URL := https://github.com/wireapp/cryptobox-c.git

build/src/$(CRYPTOBOX):
	mkdir -p build/src
	cd build/src && \
	git clone $(CRYPTOBOX_GIT_URL) $(CRYPTOBOX) && \
	cd $(CRYPTOBOX) && \
	git checkout develop
