pipeline {
  agent {
    kubernetes {
      cloud params.JENKINS_AGENT_CLOUD_NAME
      label "jenkins-slave-${UUID.randomUUID().toString()}"
      serviceAccount params.JENKINS_AGENT_SERVICE_ACCOUNT
      defaultContainer 'jnlp'
      yaml """
      apiVersion: v1
      kind: Pod
      metadata:
        labels:
          app: "jenkins-${env.JOB_BASE_NAME}"
          koji-pipeline-kind: "koji-hub-dev-pipeline"
          koji-pipeline-build-number: "${env.BUILD_NUMBER}"
      spec:
        containers:
        - name: jnlp
          image: "${params.JENKINS_AGENT_IMAGE}"
          imagePullPolicy: Always
          tty: true
          // env:
          // - name: REGISTRY_CREDENTIALS
          //   valueFrom:
          //     secretKeyRef:
          //       name: "${params.CONTAINER_REGISTRY_CREDENTIALS}"
          //       key: '.dockerconfigjson'
          resources:
            requests:
              memory: 768Mi
              cpu: 300m
            limits:
              memory: 1Gi
              cpu: 500m
      """
    }
  }
  // options {
  //   timestamps()
  //  timeout(time: 30, unit: 'MINUTES')
  // }
  environment {
    PIPELINE_NAMESPACE = readFile('/run/secrets/kubernetes.io/serviceaccount/namespace').trim()
    // PIPELINE_USERNAME = sh(returnStdout: true, script: 'id -un').trim()
  }
  stages {
    stage('Prepare') {
      steps {
        script {
          dir ("$KOJI_REPO_DIR"){
                def scmVars = checkout([$class: 'GitSCM',
                branches: [[name: params.KOJI_GIT_REF]],
                userRemoteConfigs: [[url: params.KOJI_GIT_REPO]],
                ])
                def commitHash = scmVars.GIT_COMMIT
                env.KOJI_GIT_COMMIT = commitHash
          }
        }
      }
    }
    stage('Run unit tests') {
      steps {
        // run unit tests
        sh(script: 'sed -i "/    \\/usr\\/\\*/d" /usr/local/src/koji/.coveragerc')
        sh(script: 'cd /usr/local/src/koji && make test')
      }
    }
    stage('Build container') {
      environment {
        BUILDCONFIG_INSTANCE_ID = "kojihub-container-build-${currentBuild.id}"
      }
      steps {
        script {
          openshift.withCluster() {
            // OpenShift BuildConfig doesn't support specifying a tag name at build time.
            // We have to create a new BuildConfig for each container build.
            // Create a BuildConfig from a seperated Template.
            echo 'Creating a BuildConfig for container build...'
            def template = readYaml file: 'openshift/koji-hub-container-template.yaml'
            def processed = openshift.process(template,
              "-p", "KOJI_DOJO_REMOTE=${params.KOJI_DOJO_GIT_REPO}",
              "-p", "KOJI_DOJO_BRANCH=${params.KOJI_DOJO_MAIN_BRANCH}",
              "-p", "KOJI_GIT_COMMIT=${env.KOJI_GIT_COMMIT}",
            )
            def created = openshift.apply(processed)
            def bc = created.narrow('bc')
            echo 'Starting a container build from the created BuildConfig...'
            buildSelector = bc.startBuild()
            // `buildSelector.logs()` can be dumb when the OpenShift Build is not started.
            // Let's wait for it to be started or completed (failed).
            echo 'Waiting for the container build to be started...'
            timeout(5) { // 5 min
              buildSelector.watch {
                return !(it.object().status.phase in ["New", "Pending", "Unknown"])
              }
            }
            echo 'Following container build logs...'
            // This function sometimes hangs infinitely.
            // Not sure it is a problem of OpenShift Jenkins Client plugin
            // or OpenShift.
            // FIXME: logs() step may fail with unknown reasons.
            timeout(time: 15, activity: false) {
              buildSelector.logs('-f')
            }
            // Ensure the build is stopped
            echo 'Waiting for the container build to be fully stopped...'
            timeout(5) { // 5 min
              buildSelector.watch {
                return it.object().status.phase != "Running"
              }
            }
            // Assert build result
            def ocpBuild = buildSelector.object()
            if (ocpBuild.status.phase != "Complete") {
              error("Failed to build container image for ${env.TEMP_TAG}, .status.phase=${ocpBuild.status.phase}.")
            }
            echo 'Container build is complete.'
            env.RESULTING_IMAGE_REF = ocpBuild.status.outputDockerImageReference
            env.RESULTING_IMAGE_DIGEST = ocpBuild.status.output.to.imageDigest
            def imagestream= created.narrow('is').object()
            env.RESULTING_IMAGE_REPO = imagestream.status.dockerImageRepository
            env.RESULTING_TAG = env.TEMP_TAG
          }
        }
      }
      post {
        cleanup {
          script {
            openshift.withCluster() {
              echo 'Tearing down...'
              openshift.selector('bc', [
                'app': env.BUILDCONFIG_INSTANCE_ID,
                'template': 'koji-hub-container-template',
                ]).delete()
            }
          }
        }
      }
    }
    stage('Run functional tests') {
      steps {
              // temporarily set functional test to PASSED as no testcase yet
              echo 'Functional test is set to PASSED temporarily.'
            }
          }
    stage('Push container') {
      when {
        expression {
          return params.FORCE_PUBLISH_IMAGE == 'true' ||
            params.KOJI_GIT_REF == params.KOJI_MAIN_BRANCH
        }
      }
      steps {
        script {
          def destinations = env.KOJI_DEV_IMAGE_DESTINATIONS ?
            env.KOJI_DEV_IMAGE_DESTINATIONS.split(',') : []
          openshift.withCluster() {
              def sourceImage = env.RESULTING_IMAGE_REPO + ":" + env.RESULTING_TAG
            if (env.REGISTRY_CREDENTIALS) {
               dir ("${env.HOME}/.docker") {
                    writeFile file:'config.json', text: env.REGISTRY_CREDENTIALS
               }
            }
            // pull the built image from imagestream
            echo "Pulling container from ${sourceImage}..."
            def registryToken = readFile(file: '/var/run/secrets/kubernetes.io/serviceaccount/token')
            withEnv(["SOURCE_IMAGE_REF=${sourceImage}", "TOKEN=${registryToken}"]) {
              sh '''set -e +x # hide the token from Jenkins console
              mkdir -p _build
              skopeo copy \
                --src-cert-dir=/var/run/secrets/kubernetes.io/serviceaccount/ \
                --src-creds=serviceaccount:"$TOKEN" \
                docker://"$SOURCE_IMAGE_REF" dir:_build/kojihub_container
              '''
            }
            // push to registries
            def pushTasks = destinations.collectEntries {
              ["Pushing ${it}" : {
                def dest = it
                // Only docker and atomic registries are allowed
                if (!it.startsWith('atomic:') && !it.startsWith('docker://')) {
                  dest = 'docker://' + it
                }
                echo "Pushing container to ${dest}..."
                withEnv(["DEST_IMAGE_REF=${dest}"]) {
                  /* Pushes to the internal registry can sometimes randomly fail
                  * with "unknown blob" due to a known issue with the registry
                  * storage configuration. So we retry up to 5 times. */
                  retry(5) {
                    sh 'skopeo copy dir:_build/kojihub_container "$DEST_IMAGE_REF"'
                  }
                }
              }]
            }
            parallel pushTasks
          }
        }
      }
    }
    stage('Tag into image stream') {
      when {
        expression {
          return "${params.KOJIHUB_DEV_IMAGE_TAG}" && params.TAG_INTO_IMAGESTREAM == "true" &&
            (params.FORCE_PUBLISH_IMAGE == 'true' || params.KOJI_GIT_REF == params.KOJI_MAIN_BRANCH)
        }
      }
      steps {
        script {
          openshift.withCluster() {
            openshift.withProject("${params.KOJIHUB_IMAGESTREAM_NAMESPACE}") {
              def sourceRef = "${params.KOJIHUB_IMAGESTREAM_NAME}:${env.RESULTING_TAG}"
              def destRef = "${params.KOJIHUB_IMAGESTREAM_NAME}:${params.KOJIHUB_DEV_IMAGE_TAG}"
              echo "Tagging ${sourceRef} as ${destRef}..."
              openshift.tag("${sourceRef}", "${destRef}")
            }
          }
        }
      }
    }
  }
  post {
    cleanup {
      script {
        if (env.RESULTING_TAG) {
          echo "Removing tag ${env.RESULTING_TAG} from the ImageStream..."
          openshift.withCluster() {
            openshift.withProject("${params.KOJIHUB_IMAGESTREAM_NAMESPACE}") {
              openshift.tag("${params.KOJIHUB_IMAGESTREAM_NAME}:${env.RESULTING_TAG}",
                "-d")
            }
          }
        }
      }
    }
  }
}
