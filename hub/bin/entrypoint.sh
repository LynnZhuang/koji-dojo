#!/bin/bash
set -x

build-koji.sh || exit 1

httpd -D FOREGROUND
