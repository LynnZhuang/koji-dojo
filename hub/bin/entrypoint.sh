#!/bin/bash
set -x

setup_psql.sh || exit 1

mkuser.sh kojiweb admin
mkuser.sh kojiadmin admin
mkuser.sh testadmin admin
mkuser.sh testuser

mkuser.sh kojibuilder builder

rm -rf /run/httpd/httpd.pid

httpd -D FOREGROUND
