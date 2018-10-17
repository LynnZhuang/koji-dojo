#!/bin/bash
set -x

IP=$(find-ip.py)

#echo "SSHd listening on: ${IP}:22"
#/usr/sbin/sshd -D

echo "Koji client environment started on ${IP}"
# This won't work in openshift
# if [ -n "$KOJI_HUB" ]; then
# 	while true; do sleep 10000; done
# else
# 	exec /bin/bash -l
# fi
