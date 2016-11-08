# Hibernate OGM

*Version: 5.1.0.Beta1 - 08-11-2016*

## Description

Hibernate OGM is an attempt to store data in a NoSQL data grid using the Hibernate ORM engine rather than rewriting a JPA engine from scratch.

The benefits are fairly obvious:
 - reimplementing the complex JPA specification is a lot of work
 - a new implementation would mature at a rather slow rate and risk of bugs would be high
 - Hibernate is familiar to many people

Checkout <http://ogm.hibernate.org> for more information.

## Build instructions

The code is available on GitHub at <https://github.com/hibernate/hibernate-ogm>.

To run the full project build including tests for all backends, documentation etc. execute:

    mvn clean install -s settings-example.xml

Note that for running the test suite against separately installed MongoDB and CouchDB servers their host name must be specified via an environment variable.
See the sections below for the details.

To speed things up, there are several options for skipping parts of the build.
To run the minimum project build without integration tests, documentation and distribution execute:

    mvn clean install -DskipITs -DskipDocs -DskipDistro -s settings-example.xml

The following sections describe these options in more detail.

### Importing sources in Eclipse

Import the project as any standard Maven project.
This might trigger a dialog to automatically find and install additional m2e plugins: allow that.

Make sure that annotation processing is enabled in your project settings (see "Properties" - "Maven" - "Annotation Processing", the setting should be "Automatically configure JDT APT").

### Integration tests

You can skip integration tests by specifying the `skipITs` property:

    mvn clean install -DskipITs -s settings-example.xml

### Documentation

The documentation is built by default as part of the project build. You can skip it by specifying the `skipDocs` property:

    mvn clean install -DskipDocs -s settings-example.xml

If you just want to build the documentation, run it from the _documentation/manual_ subdirectory.

For rapid documentation testing, you can limit the generated format to html to speed up the process

    mvn clean install -f documentation/manual/pom.xml -s settings-example.xml -Djdocbook.format=html_single

### Distribution

The distribution bundle is built by default as part of the project build. You can skip it by specifying the `skipDistro` property:

    mvn clean install -DskipDistro -s settings-example.xml

### Integration tests

Integration tests can be run from the integrationtest module and the default behaviour is to download the WildFly application server,
unpack the modules in it and run the tests using Arquillian.

#### WARNING
Be careful when using on existing installation since the modules used by the build are going to be extracted into the
server you want to run the test, changing the original setup.

### MongoDB

For executing the tests in the _mongodb_ and _integrationtest/mongodb_ modules, by default the
[embedmongo-maven-plugin](https://github.com/joelittlejohn/embedmongo-maven-plugin) is used which downloads the MongoDB
distribution, extracts it, starts a _mongod_ process and shuts it down after test execution.

If required, you can configure the port to which the MongoDB instance binds to (by default 27018)
and the target directory for the extracted binary (defaults to _${project.build.directory}/embeddedMongoDb/extracted_) like this:

    mvn clean install -s settings-example.xml -DembeddedMongoDbTempDir=<my-temp-dir> -DembeddedMongoDbPort=<my-port>

To work with a separately installed MongoDB instance instead, specify the property `-DmongodbProvider=external`:

    mvn clean install -s settings-example.xml -DmongodbProvider=external

This assumes MongoDB to be installed on `localhost`, using the default port and no authentication.
If you work with different settings, configure the required properties in hibernate.properties (for the tests in _mongodb_)
and/or the environment variables `MONGODB_HOSTNAME` `MONGODB_PORT` `MONGODB_USERNAME` `MONGODB_PASSWORD` (for the tests in _integrationtest/mongodb_)
prior to running the tests:

    export MONGODB_HOSTNAME=mongodb-machine
    export MONGODB_PORT=1234
    export MONGODB_USERNAME=someUsername
    export MONGODB_PASSWORD=someP@ssw0rd
    mvn clean install -s settings-example.xml -DmongodbProvider=external

Finally, you also can run the test suite against the in-memory "fake implementation" Fongo:

    mvn clean install -s settings-example.xml -DmongodbProvider=fongo

### CouchDB

For running the tests in the _couchdb_ module an installed CouchDB server is required. Specify its host name by
setting the environment variable `COUCHDB_HOSTNAME` prior to running the test suite:

    export COUCHDB_HOSTNAME=couchdb-machine

If this variable is not set, the _couchdb_ module still will be compiled and packaged but the tests will be skipped.
If needed, the port to connect to can be configured through the environment variable `COUCHDB_PORT`.

### Cassandra

For running the tests in the _cassandra_ module an installed Cassandra server is required. Specify its host name by
setting the environment variable `CASSANDRA_HOSTNAME` prior to running the test suite:

    export CASSANDRA_HOSTNAME=cassandra-machine

If this variable is not set, the _cassandra_ module still will be compiled and packaged but the tests will be skipped.
If needed, the port to connect to can be configured through the environment variable `CASSANDRA_PORT`.

### Redis

For running the tests in the _redis_ module an installed Redis server is required. Specify its host name by
setting the environment variable `REDIS_HOSTNAME` prior to running the test suite:

    export REDIS_HOSTNAME=redis-machine

If this variable is not set, the _redis_ module still will be compiled and packaged but the tests will be skipped.
If needed, the port to connect to can be configured through the environment variable `REDIS_PORT`.

Tests with the _redis_ module can be started using a Makefile. The Makefile takes care of downloading and compiling
a recent Redis version, starts a single Redis Standalone and four Redis Cluster nodes and can start the tests.

     make test # Make me happy and run tests against Redis Standalone and Redis Cluster
     make test-standalone
     make test-cluster

Commands to spin up/shut down the Redis instances:

    make start
    make stop

### Neo4j

For running the tests in the _neo4j_ and _integrationtest/neo4j_ modules, by default the
embedded Neo4j configuration is used.

If you want to run the tests on a remote server, you need to specify the profile `neo4j-remote`

    mvn clean install -s settings-example.xml -Pneo4j-remote

This assumes Neo4j to be installed on `localhost`, using the default port and no authentication.
If you work with different settings, configure the required properties in hibernate.properties
and/or the environment variables `NEO4J_HOSTNAME`, `NEO4J_PORT`, `NEO4J_USERNAME` and `NEO4J_PASSWORD`
prior to running the tests:

    export NEO4J_HOSTNAME=neo4j-machine
    export NEO4J_PORT=1234
    export NEO4J_USERNAME=someUsername
    export NEO4J_PASSWORD=someP@ssw0rd

## Contact

Latest Documentation:

   <http://community.jboss.org/en/hibernate/ogm>

Bug Reports:

   Hibernate JIRA (preferred): <https://hibernate.onjira.com/browse/OGM>
   <hibernate-dev@lists.jboss.org>

Free Technical Support:

   <https://forum.hibernate.org/viewforum.php?f=31>

## Notes

If you want to contribute, come to the <hibernate-dev@lists.jboss.org> mailing list
or join us on #hibernate-dev on freenode (login required)

This software and its documentation are distributed under the terms of the
FSF Lesser Gnu Public License (see license.txt).
