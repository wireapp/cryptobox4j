#!/bin/bash

NAME="bots.runtime"

docker build -t $DOCKER_USERNAME/$NAME:2.11.0 . --rm
docker push $DOCKER_USERNAME/$NAME:2.11.0
