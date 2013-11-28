# Hibernate OGM

Version: 4.0.0.Beta4 - 25 October 2013

## Description

Hibernate OGM is an attempt to store data in a NoSQL data grid using the Hibernate ORM engine rather than rewriting a JPA engine from scratch.

The benefits are fairly obvious:
 - reimplementing the complex JPA specification is a lot of work
 - a new implementation would mature at a rather slow rate and risk of bugs would be high
 - Hibernate is familiar to many people

## Instructions

Checkout <http://ogm.hibernate.org for more information>
The code is available on GitHub at <https://github.com/hibernate/hibernate-ogm>

To build the project, run

    mvn clean install -s settings-example.xml

### Integration tests

You can skip integration tests by specifying the `skipITs` property:

    mvn clean install -DskipITs -s settings-example.xml

or

    mvn clean install -DskipITs=true -s settings-example.xml

### Documentation

The documentation is built by default as part of the project build. You can skip it by specifying the `skipDocs` property:

    mvn clean install -DskipDocs=true -s settings-example.xml

If you just want to build the documentation, run it from the `hibernate-ogm-documentation/manual` subdirectory.

For rapid documentation testing, you can limit the generated format to html to speed up the process

    mvn clean install -f hibernate-ogm-documentation/manual/pom.xml -s settings-example.xml -Djdocbook.format=html_single

### Distribution

The distribution bundle is built by default as part of the project build. You can skip it by specifying the `skipDistro` property:

    mvn clean install -DskipDistro=true -s settings-example.xml

### MongoDB

For executing the tests in the _hibernate-ogm-mongodb_ and _hibernate-ogm-integrationtest-mongodb_ modules, the
[embedmongo-maven-plugin](https://github.com/joelittlejohn/embedmongo-maven-plugin) is used which downloads the MongoDB
distribution, extracts it, starts a _mongod_ process and shuts it down after test execution.

If required, you can configure the port to which the MongoDB instance binds to (by default 27018)
and the target directory for the extracted binary (defaults to _${project.build.directory}/embeddedMongoDb/extracted_) like this:

    mvn clean install -s settings-example.xml -DembeddedMongoDbTempDir=<my-temp-dir> -DembeddedMongoDbPort=<my-port>

To work with a separately installed MongoDB instance instead, specify the 'useExternalMongoDb' property:

    mvn clean install -s settings-example.xml -DuseExternalMongoDb

This assumes MongoDB to be installed on `localhost`, using the default port. If you have MongoDB
installed on another host or use a different port, you can set the environment variables
`MONGODB_HOSTNAME` `MONGODB_PORT` and then run the test suite:

    export MONGODB_HOSTNAME=mongodb-machine
    export MONGODB_PORT=1234
    mvn clean install -s settings-example.xml

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
