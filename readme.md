# Hibernate OGM

Version: 4.0.0-SNAPSHOT

## Description

Hibernate OGM is an attempt to store data in a NoSQL data grid using he Hibernate Core engine rather than rewriting a JPA engine from scratch.

The benefits are fairly obvious:
 - reimplementing the complex JPA specification is a lot of work
 - a new implementation would mature at a rather slow rate and risk of bugs would be high
 - Hibernate is familiar to many people

## Instructions

Checkout <http://hibernate.org/subprojects/ogm for more information>
The code is available on GitHub at <https://github.com/hibernate/hibernate-ogm>

To build the project, run

    mvn clean install -s settings-example.xml

### MongoDB

If you have MongoDB installed on `localhost` and the default port, use the mongodb profile as well

    mvn clean install -s settings-example.xml -Pmongodb

If you have MongoDB installed in a non default host / port, you can set the environment variables
and then run the test suite

    export MONGODB_HOST=mongodb-machine
    export MONGODB_PORT=1234
    mvn clean install -s settings-example.xml -Pmongodb


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
