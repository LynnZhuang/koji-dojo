#!/bin/bash

set -x
ls -la /opt/koji/

psql="PGPASSFILE=/opt/koji/.pgpass psql --host=koji-db --username=koji koji"

cat /usr/local/src/koji/docs/schema.sql | $psql
echo "BEGIN WORK; INSERT INTO content_generator(name) VALUES('test-cg'); COMMIT WORK;" | $psql
