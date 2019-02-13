# Jenkins Consistency-Checking Plug-in (forked from TAP plugin)

# to build:
export MAVEN_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,address=8000,suspend=n"
mvn hpi:run (for testing 'live')
# or
mvn clean install -Dmaven.test.skip=true -Denforcer.skip=true (yes, yes, ugly)
