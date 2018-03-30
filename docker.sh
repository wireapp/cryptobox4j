#!/bin/bash

NAME="bots.runtime"

docker build -t dejankovacevic/$NAME:2.10.0 .
docker push dejankovacevic/$NAME
