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
RUN mkdir -p /usr/local/src \
  && git clone "$KOJI_GIT_REPO" /usr/local/src/koji \
  && cd /usr/local/src/koji \
  && git fetch origin "$KOJI_GIT_REF" \
  && git checkout -f "$KOJI_GIT_REF" \
  && rm -rf noarch \
  && make test-rpm \
  && yum -y localinstall noarch/koji-hub*.rpm noarch/koji-1.*.rpm noarch/koji-web*.rpm noarch/python2-koji*.rpm \
  && yum clean all

ADD etc/ /etc/
ADD bin/ /usr/local/bin/
ADD root/ /root/

RUN chmod 600 /root/.pgpass
RUN chmod +x /usr/local/bin/*

ADD cgi/*.py /var/www/cgi-bin/
RUN chmod o+rx /var/log /var/log/httpd
RUN chmod +x /var/www/cgi-bin/*.py
RUN chmod o+rwx /var/www/html
RUN chmod -R o+rx /etc/httpd

RUN mkdir -p /mnt/koji/{packages,repos,work,scratch}
RUN chown apache.apache /mnt/koji/*
RUN echo 'root:mypassword' | chpasswd

EXPOSE 22 80 443

ENTRYPOINT /usr/local/bin/entrypoint.sh
