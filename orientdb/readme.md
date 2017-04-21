# OrientDB Integration with Hibernate OGM

## Description

This module contains the integration point between [OrientDB](http://orientdb.com/orientdb/)
and Hibernate OGM.

At the moment it is just the inital scaffolding and some initial experimental code,
we are looking for contributors to help us complete the integration.

## Build instructions

From the root of the whole Hibernate OGM project you can run

    mvn -pl orientdb -am [-s settings-example.xml]

This will only consider the OrientDB module speeding up the process.

Don't be scared about the test failure during the build, those are suppose to happen
since teh dialect has not been implemented yet.

We usually discuss the integration on JIRA,
[issue OGM-855](https://hibernate.atlassian.net/browse/OGM-855), 
[HipChat](https://www.hipchat.com/gUrNEkkdR).

Feel free to join the discussion, give suggestions or ask questions!

## There is a lot of stuff, where should I start?

### Check the website

- [Check the documentation](http://docs.jboss.org/hibernate/ogm/5.0/reference/en-US/html/ch01.html)
- [Check the website](http://hibernate.org/ogm/contribute/)

### Which class does provide the connection to OrientDB?

`OrientDBDatastoreProvider` is the class responsible for starting the database and provide access
to the database. Once this class is ready it will be possible to connect to the db and start
to execute test cases. OrientDB comes with a JDBC Driver, the `MongoDBDataStoreProvider` might be
a good place to check for inspiration.

### How to run the tests from the IDE

The tests will look for the methods in `OrientDBTestHelper`, these are used to count the data in
the store and make sure that it does not contain any unexpected result. It will also destroy the
database when required.

I think the easiest tests to start with are `CRUDTest` and `BuiltInTypeTest`; because these tests
are in a separate project, if you want to run them from your IDE you can choose one of the following
approaches:

1. Copy the test class in the project temporarly
2. Create a test class in the project that extends the class you want to test (see `OrientDBTest`)
3. Check `OrientDBBackendTckHelper`

### The Dialect

`OrientDBDialect` is the class the define how we interact with database, it's better to start
with the methods called `*Tuple` and skip the association ones (for now). Once they are implemented
it will be possible to store and get entities without associations.

### Similar Datastores

OrientDB uses a multi-model engine, since we connect with a JDBC driver and we can store Document
the MongoDB module might be a good place for inspiration. For the relationship part (more complex),
you can check the Neo4j one.

## TL;DR

* Please, help us;
* Clone the repository;
* mvn -pl orientdb -am -s settings-example.xml
* it fails, start to hack;
* ask questions;
* propose changes.

