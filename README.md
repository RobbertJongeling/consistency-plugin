# Jenkins Consistency-Checking Plug-in 
(forked from TAP plugin https://github.com/jenkinsci/tap-plugin)

# to build and test:
`export MAVEN_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,address=8000,suspend=n"`
`mvn hpi:run`
# or build and then manually install plugin in your local Jenkins instance:
`mvn clean install -Dmaven.test.skip=true -Denforcer.skip=true`
