pipeline {
  agent {
    kubernetes {
    cloud "${params.JENKINS_AGENT_CLOUD_NAME}"
    label "jenkins-slave-${UUID.randomUUID().toString()}"
    serviceAccount "${params.JENKINS_AGENT_SERVICE_ACCOUNT}"
    defaultContainer 'jnlp'
    yaml """
    apiVersion: v1
    kind: Pod
    metadata:
      labels:
        app: "${env.JOB_BASE_NAME}"
        kojihub-pipeline-kind: "kojihub-integration-test-pipeline"
        kojihub-pipeline-build-number: "${env.BUILD_NUMBER}"
    spec:
      containers:
      - name: jnlp
        image: "${params.JENKINS_AGENT_IMAGE}"
        imagePullPolicy: Always
        tty: true
        env:
        - name: REGISTRY_CREDENTIALS
          valueFrom:
            secretKeyRef:
              name: "${params.CONTAINER_QUAY_CREDENTIALS}"
              key: '.dockerconfigjson'
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
  stages {
    stage('Run functional tests') {
      echo "The test ID is: ${TEST_ID}"
      environment {
        // Jenkins BUILD_TAG could be too long (> 63 characters) for OpenShift to consume
        TEST_ID = "${params.TEST_ID ?: 'jenkins-' + currentBuild.id}"
        ENVIRONMENT_LABEL = "test-${env.TEST_ID}"
      }
      steps {
        echo "Container image ${params.IMAGE} will be tested."
        script {
          openshift.withCluster() {
            def imageTag = (params.IMAGE =~ /(?::(\w[\w.-]{0,127}))?$/)[0][1]
            def imageRepo = imageTag ? params.IMAGE.substring(0, params.IMAGE.length() - imageTag.length() - 1) : params.IMAGE
            def template = readYaml file: 'openshift/koji-hub-deploy-template.yaml'
            def webPodReplicas = 1 // The current quota in UpShift is agressively limited
            def models = openshift.process(template,
              '-p', "KOJI_HUB_IMAGE_REPO=${imageRepo}",
              '-p', "KOJI_HUB_VERSION=${imageTag ?: 'latest'}",
            )
            def objects = openshift.apply(models)
            echo "Waiting for test pods with label environment=${env.ENVIRONMENT_LABEL} to become Ready"
            //def rm = dcSelector.rollout()
            def dcs = openshift.selector('dc', ['environment': env.ENVIRONMENT_LABEL])
            def rm = dcs.rollout()
            def pods = openshift.selector('pods', ['environment': env.ENVIRONMENT_LABEL])
            timeout(15) {
              pods.untilEach(webPodReplicas + 1) {
                def pod = it.object()
                if (pod.status.phase in ["New", "Pending", "Unknown"]) {
                  return false
                }
                if (pod.status.phase == "Running") {
                  for (cond in pod.status.conditions) {
                      if (cond.type == 'Ready' && cond.status == 'True') {
                          return true
                      }
                  }
                  return false
                }
                error("Test pod ${pod.metadata.name} is not running. Current phase is ${pod.status.phase}.")
              }
            }
            // Run functional tests
            echo 'Not started yet...'
          }
        }
      }
      post {
        always {
          script {
            openshift.withCluster() {
              /* Extract logs for debugging purposes */
              openshift.selector('deploy,pods', ['environment': env.ENVIRONMENT_LABEL]).logs()
            }
          }
        }
        cleanup {
          script {
            openshift.withCluster() {
              /* Tear down everything we just created */
              echo "Tearing down test resources..."
              openshift.selector('dc,deploy,configmap,secret,svc,route',
                      ['environment': env.ENVIRONMENT_LABEL]).delete()
            }
          }
        }
      }
    }
  }
}
