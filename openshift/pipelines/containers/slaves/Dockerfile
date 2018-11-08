#FIXME: The Pipeline is currently using this Dockerfile to produce images. The one located at the project root is not changed in case of breaking anything.
FROM docker-registry.engineering.redhat.com/devops-automation/rad-slave-fedora-27:latest
LABEL name="koji-jenkins-slave" \
      description="Jenkins slave for Koji Hub dev tests" \
      vendor="Koji Developers" \
      license="GPLv2+"

USER root

# Add dependency Tini, a tiny but valid init for containers
ARG TINI_VERSION=0.18.0
# OpenShift Jenkins Pipeline (DSL) Plugin requires oc binary to be present in the PATH environment variable of all Jenkins nodes
ARG OPENSHIFT_CLIENTS_VERSION=3.10.0
ARG OPENSHIFT_CLIENTS_COMMIT=dd10d17

RUN dnf -y install 'dnf-command(builddep)' dnf-utils mock-core-configs skopeo \
    wget postgresql make rpmdevtools rpmlint \
    python3-flake8 python3-pylint python3-pytest \
    python3-sphinx python3-sphinxcontrib-httpdomain \
    python-coverage python-cheetah python-requests \
    python-rpm python-mock python-multilib  mock yum \
    python-psycopg2 python-qpid-proton python-kerberos \
    gcc python-devel python-gssapi krb5-devel \
    python-krbV python-dateutil python-nose\
  && pip install gssapi requests_kerberos \
  && mkdir -p /usr/local/src/koji \
  && TEMPDIR=$(mktemp -d) \
  # install oc
  && wget -O "$TEMPDIR"/openshift-origin-client-tools.tar \
    "https://github.com/openshift/origin/releases/download/v${OPENSHIFT_CLIENTS_VERSION}/openshift-origin-client-tools-v${OPENSHIFT_CLIENTS_VERSION}-${OPENSHIFT_CLIENTS_COMMIT}-linux-64bit.tar.gz" \
  && tar -xvf "$TEMPDIR"/openshift-origin-client-tools.tar -C "$TEMPDIR" --strip-components=1 \
  && cp "$TEMPDIR"/oc /usr/local/bin/oc && chmod +rx /usr/local/bin/oc \
  # install tini
  && wget -O /usr/local/bin/tini "https://github.com/krallin/tini/releases/download/v${TINI_VERSION}/tini" \
  && chmod +rx /usr/local/bin/tini \
  # install wait-for-it.sh, to allow containers to wait for other services to come up
  && wget -O /usr/local/bin/wait-for-it "https://raw.githubusercontent.com/vishnubob/wait-for-it/master/wait-for-it.sh" \
  && chmod +rx /usr/local/bin/tini /usr/local/bin/wait-for-it \
  # clean up
  && rm -rf "$TEMPDIR" \
  && dnf clean all

# install build dependencies for Koji
COPY koji.spec /usr/local/src/koji/koji.spec
RUN cd /usr/local/src/koji \
  && dnf -y builddep koji.spec \
  && dnf clean all \
  && chgrp -R root /usr/local/src/koji \
  && chmod -R g+rwX /usr/local/src/koji

WORKDIR /var/lib/jenkins/
ENTRYPOINT ["/usr/local/bin/tini", "--", "jenkins-slave"]
USER 1000
