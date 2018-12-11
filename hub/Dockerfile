FROM centos:centos7
MAINTAINER John Casey <jcasey@redhat.com>

ARG KOJI_RPM=koji
ARG KOJI_HUB_RPM=koji-hub
ARG KOJI_HUB_PLUGINS_RPM=koji-hub-plugins
ARG RH_KOJI_PLUGINS_RPM=rh-koji-plugins
ARG BREWKOJI_RPM=brewkoji
ARG KOJI_WEB_RPM=koji-web
ARG RH_KOJI_THEME_RPM=rh-koji-theme
ARG ADDITIONAL_RPMS='python-simplejson httpd mod_ssl PyGreSQL'
ARG ADDITIONAL_REPOS=

ARG KOJI_GIT_REPO=https://pagure.io/koji.git
ARG KOJI_GIT_REF=master
ARG KOJI_GIT_COMMIT
ARG PROJ_NAME

#RUN sed -i '/excludedocs/d' /etc/rpm/macros.imgcreate
RUN sed -i '/nodocs/d' /etc/yum.conf

VOLUME ["/opt/koji-clients", "/opt/koji"]

RUN yum -y update && \
#    yum install -y http://download.fedoraproject.org/pub/epel/6/x86_64/epel-release-6-8.noarch.rpm && \
    yum -y install \
        epel-release \
        git \
        yum-utils \
        tar \
        bzip2 \
        rpm-build \
        make \
        patch \
        httpd \
        mod_wsgi \
        mod_ssl \
        lsof \
        python-simplejson \
        PyGreSQL \
        pyOpenSSL \
        python-backports \
        python-backports-ssl_match_hostname \
        python-cheetah \
        python-coverage \
        python-dateutil \
        python-devel \
        python-kerberos \
        python-krbV \
        python-qpid \
        python-saslwrapper \
        saslwrapper \
        postgresql \
        sudo \
        mod_auth_kerb \
        python-cheetah \
        python-markdown \
        python-pygments \
        python-setuptools \
        python-sphinx \
        python-coverage \
        openssh-server \
        wget \
    ; yum clean all

# install
RUN echo $(echo $KOJI_GIT_COMMIT)
RUN echo $(echo $KOJI_GIT_REPO)
RUN mkdir -p /usr/local/src \
  && git clone "$KOJI_GIT_REPO" /usr/local/src/koji \
  && cd /usr/local/src/koji \
  && git fetch origin "$KOJI_GIT_REF" \
  # && git checkout -f $KOJI_GIT_COMMIT \
  # RUN echo $(ls -l /usr/local/src/koji/)
  # modify httpd port, 80 to 8080
  && sed -i "s/port == '80'/port == '8080'/g" ./www/kojiweb/index.py \
  && rm -rf noarch \
  && make test-rpm \
  && yum -y localinstall noarch/koji-hub*.rpm noarch/koji-1.*.rpm noarch/koji-web*.rpm noarch/python2-koji*.rpm \
  && yum clean all

ADD etc/ /etc/
ADD bin/ /usr/local/bin/
ADD root/ /root/

RUN chmod 666 /root/.pgpass
RUN chmod +x /usr/local/bin/*

ADD cgi/*.py /var/www/cgi-bin/
RUN chmod o+rx /var/log /var/log/httpd
RUN chmod +x /var/www/cgi-bin/*.py
RUN chmod o+rwx /var/www/html
RUN chmod -R o+rx /etc/httpd

# RUN bash -c "/usr/local/bin/setup_psql.sh"
RUN bash -c "/usr/local/bin/setup.sh $PROJ_NAME"
RUN echo 'root:mypassword' | chpasswd

# RUN rm -rf /etc/httpd/conf.d/ssl.conf
RUN chgrp root /run/httpd && chmod -R g+w /run/httpd
RUN chmod -R 775 /etc/koji-hub && chmod -R 775 /mnt/koji
RUN chmod -R 775 /etc
RUN chmod -R 775 /var/www/html
RUN chmod -R 775 /etc/httpd/logs
# RUN chcon -R -t public_content_rw_t /mnt/koji/*

# temporary
RUN chmod -R 775 /usr/lib/python2.7/site-packages/koji_cli
RUN chmod -R 775 /usr/lib/python2.7/site-packages/koji_cli_plugins
RUN chmod -R 775 /usr/lib/python2.7/site-packages/koji
RUN chmod -R 775 /usr/share/koji-web

# as port 80 is not available in OpenShift, we temporary use 8080 here
RUN sed -i 's/Listen 80/Listen 8080/g' /etc/httpd/conf/httpd.conf

#

# configure
RUN \
  # redirect HTTPD logs to stdout/stderr
  sed -ri \
  -e 's!^(\s*CustomLog)\s+\S+!\1 /proc/self/fd/1!g' \
  -e 's!^(\s*ErrorLog)\s+\S+!\1 /proc/self/fd/2!g' \
  /etc/httpd/conf/httpd.conf

EXPOSE 22 80 443

ENTRYPOINT /usr/local/bin/entrypoint.sh
