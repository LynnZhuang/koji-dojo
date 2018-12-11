#!/bin/bash
set -x

setup_psql.sh || exit 1

rm -rf /run/httpd/httpd.pid

httpd -D FOREGROUND
