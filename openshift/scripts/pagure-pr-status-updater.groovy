import java.net.URLEncoder
class PagureClient {
  String pagureApiUrl
  String token
  def steps
  def callApi(String httpMode, String apiPath, Map payload = null) {
    def headers = []
    if (token) {
      headers << [name: 'Authorization', value: "token ${token}", maskValue: true]
    }
    def payloadItems = []
    if (payload) {
      for (it in payload) {
        if (it == null || it.value == null) {
          continue
        }
        payloadItems << (URLEncoder.encode(it.key.toString(), 'utf-8') +
          '=' +  URLEncoder.encode(it.value.toString(), 'utf-8'))
      }
    }
    return steps.httpRequest(
      httpMode: httpMode,
      url: "${pagureApiUrl}/${apiPath}",
      acceptType: 'APPLICATION_JSON',
      contentType: 'APPLICATION_FORM',
      requestBody: payloadItems.join('&'),
      customHeaders: headers,
    )
  }
  def getPR(Map args) {
    def apiPath = "${args.fork?'fork/':''}${args.repo}/pull-request/${args.pr}"
    def response = callApi('GET', apiPath)
    return steps.readJSON(text: response.content)
  }
  def updatePRStatus(Map args) {
    def apiPath = "${args.fork?'fork/':''}${args.repo}/pull-request/${args.pr}/flag"
    def response = callApi('POST', apiPath, [
      'username': args.username,
      'uid': args.uid,
      'percent': args.percent,
      'comment': args.comment,
      'url': args.url,
    ])
    return steps.readJSON(text: response.content)
  }
  def commentOnPR(Map args) {
    def apiPath = "${args.fork?'fork/':''}${args.repo}/pull-request/${args.pr}/comment"
    def response = callApi('POST', apiPath, [
      'comment': args.comment,
    ])
    return steps.readJSON(text: response.content)
  }
}
def getPagurePRInfo() {
  def pagureClient = new PagureClient (pagureApiUrl: env.PAGURE_API, steps: steps)
  return pagureClient.getPR(fork: env.PAGURE_REPO_IS_FORK == 'true', repo: "${PAGURE_REPO_NAME}", pr: env.PR_NO)
}
def setBuildStatusOnPagurePR(percent, String comment) {
  withCredentials([string(credentialsId: "${env.PIPELINE_NAMESPACE}-${PAGURE_API_KEY_SECRET_NAME}", variable: 'TOKEN')]) {
    def pagureClient = new PagureClient (pagureApiUrl: env.PAGURE_API, token: env.TOKEN, steps: steps)
    pagureClient.updatePRStatus(
      username: 'c3i-jenkins', uid: 'ci-pre-merge', url: env.DEV_BUILD_URL?:env.BUILD_URL,
      percent: percent, comment: comment, pr: env.PR_NO,
      repo: "${PAGURE_REPO_NAME}", fork: env.PAGURE_REPO_IS_FORK == 'true')
  }
}
def commentOnPR(String comment) {
  withCredentials([string(credentialsId: "${env.PIPELINE_NAMESPACE}-${PAGURE_API_KEY_SECRET_NAME}", variable: 'TOKEN')]) {
    def pagureClient = new PagureClient (pagureApiUrl: env.PAGURE_API, token: env.TOKEN, steps: steps)
    pagureClient.commentOnPR(
      comment: comment, pr: env.PR_NO,
      repo: "${PAGURE_REPO_NAME}", fork: env.PAGURE_REPO_IS_FORK == 'true')
  }
}
def sendBuildStatusEmail(boolean success) {
  def status = success ? 'passed' : 'failed'
  def reciepent = env.PAGURE_POLLING_FOR_PR != 'true' && ownership.job.ownershipEnabled && ownership.job.primaryOwnerEmail ?
    ownership.job.primaryOwnerEmail : env.GIT_AUTHOR_EMAIL
  def subject = "Jenkins job ${env.JOB_NAME} #${env.BUILD_NUMBER} ${status}."
  def body = "Build URL: ${env.DEV_BUILD_URL}"
  if (env.PAGURE_POLLING_FOR_PR == 'true') {
    subject = "Jenkins job ${env.JOB_NAME}, PR #${env.PR_NO} ${status}."
    body += "\nPull Request: ${env.PR_URL}"
  }
  emailext to: reciepent, subject: subject, body: body
}
