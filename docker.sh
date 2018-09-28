#!/bin/bash

NAME="bots.runtime"

docker build -t dejankovacevic/$NAME:2.10.1 .
docker push dejankovacevic/$NAME
