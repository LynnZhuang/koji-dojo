#!/bin/bash
set -x

# build-koji.sh || exit 1

rm -rf /run/httpd/httpd.pid

httpd -D FOREGROUND
