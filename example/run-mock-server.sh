#!/usr/bin/env bash

EXAMPLE_DIR=$(cd $(dirname $0); pwd)

docker run -it --rm \
    -p 8080:8080 \
    --name wiremock \
    -v ${EXAMPLE_DIR}/wiremock:/home/wiremock \
    wiremock/wiremock:2.34.0 \
    --verbose
