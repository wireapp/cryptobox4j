#!/bin/bash

NAME="bots.runtime"

docker build -t $DOCKER_USERNAME/$NAME:2.10.3 .
docker push $DOCKER_USERNAME/$NAME
