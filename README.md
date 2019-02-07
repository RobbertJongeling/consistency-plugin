# Jenkins TAP Plug-in

[![Build Status](https://jenkins.ci.cloudbees.com/buildStatus/icon?job=plugins/tap-plugin)](https://jenkins.ci.cloudbees.com/job/plugins/job/tap-plugin/)

[https://wiki.jenkins-ci.org/display/JENKINS/TAP+Plugin](https://wiki.jenkins-ci.org/display/JENKINS/TAP+Plugin)



export MAVEN_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,address=8000,suspend=n"
mvn hpi:run (for testing 'live')

mvn clean install -Dmaven.test.skip=true (for compiling completely (but not testing because those all fail now))
