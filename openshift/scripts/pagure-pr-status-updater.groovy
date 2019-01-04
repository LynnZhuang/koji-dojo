@Grab('io.github.http-builder-ng:http-builder-ng-core:1.0.3')
import static groovyx.net.http.HttpBuilder.configure
import static groovyx.net.http.ContentTypes.JSON
import groovyx.net.http.*
import static groovy.json.JsonOutput.*

// magic to get jenkins build properties
def build = this.getProperty('binding').getVariable('build')
def listener = this.getProperty('binding').getVariable('listener')
def env = build.getEnvironment(listener)
println "Environment Variables:"
println prettyPrint(toJson(env))

def setBuildStatusOnPagurePR(pagure_site, pagure_repo_name, pr_no, token, percent, build_url, String comment) {
  def pagureUrl = "/api/0/${pagure_repo_name}/pull-request/${pr_no}/flag"
  def payload = [ 'username': 'c3i-jenkins',
                'percent' : percent,
                'comment' : comment,
                'url'     : build_url,
                'uid'     : 'ci-pre-merge'
              ]
  println "Sending Args:"
  println payload

  def result = configure {
    request.uri = pagure_site
    request.contentType = JSON[0]
    request.headers['Authorization'] = "token ${token}"
  }.post {
    request.uri.path = pagureUrl
    request.body = payload
    request.contentType = 'application/x-www-form-urlencoded'
    request.encoder 'application/x-www-form-urlencoded', NativeHandlers.Encoders.&form
  }

  println prettyPrint(toJson(result))
}

def commentOnPR(pagure_site, pagure_repo_name, pr_no, token, String comment) {
  def pagureUrl = "/api/0/${pagure_repo_name}/pull-request/${pr_no}/comment"
  def payload = [ 'comment': comment]

  def result = configure {
    request.uri = pagure_site
    request.contentType = JSON[0]
    request.headers['Authorization'] = "token ${token}"
  }.post {
    request.uri.path = pagureUrl
    request.body = payload
    request.contentType = 'application/x-www-form-urlencoded'
    request.encoder 'application/x-www-form-urlencoded', NativeHandlers.Encoders.&form
  }

  println prettyPrint(toJson(result))
}
