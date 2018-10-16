#!/bin/bash

set -x
psql="psql --host=koji-db --username=koji koji"

cat /opt/koji/docs/schema.sql | $psql
echo "BEGIN WORK; INSERT INTO content_generator(name) VALUES('test-cg'); COMMIT WORK;" | $psql
