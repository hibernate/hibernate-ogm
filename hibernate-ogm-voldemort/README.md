## Voldemort Hibernate OGM Provider

This provider allows the users to use Voldemort as their datastore choice. One of the beauty of Hibernate on relational databases is that the users can change their underlying databases from one to another easily using its configuration file. This can be done on NoSQL as well. This is one of the providers on NoSQL as a part of Hibernate OGM for the purpose.

## Before typing `mvn clean install`

There is one thing that should be done before typing `mvn clean install`. Fortunately Voldemort provides itself as an embedded server, so that it can be used in unit testing. On the other hand, Voldemort doesn't provide itself in Maven repository, so that it must be installed into `~/.m2` directory to run the command.

## Install Voldemort

Here are the steps to install Voldemort to maven repository manually.
1. Download the latest version of Voldemort from [Voldemort Download](https://github.com/voldemort/voldemort/downloads). This provider uses Voldemort-0.90.1.1. 
2. Type the commands to install the dependencies manually with Maven such as below.
`mvn install:install-file -Dfile=/path/to/voldemort-0.90.1/dist/voldemort-0.90.1.jar -DgroupId=voldemort -DartifactId=voldemort-core -Dversion=0.90.1 -Dpackaging=jar -DgeneratePom=true`

`mvn install:install-file -Dfile=/path/to/voldemort-0.90.1/dist/voldemort-contrib-0.90.1.jar -DgroupId=voldemort -DartifactId=voldemort-contrib -Dversion=0.90.1 -Dpackaging=jar -DgeneratePom=true`

## Configure Voldemort for testings.

Voldemort is highly configurable and to use it for unit testing and integration testing, it should be configured for the purposes. Three configuration samples are supplied with Voldemort installation. This provider uses one of the modified configuration for testings. Please check `VOLDEMORT_HOME/config` for the configuration samples and [Voldemort configuration](http://project-voldemort.com/configuration.php) for the details. Here is the configuration that this provider uses for testings.

### cluster.xml
    <cluster>
    <name>HibernateOGM</name>
    <server>
        <id>0</id>
        <host>localhost</host>
        <http-port>8081</http-port>
        <socket-port>6666</socket-port>
        <admin-port>6667</admin-port>
        <partitions>0, 1</partitions>
    </server>
    </cluster>

### stores.xml
    <!-- Please do not modify key-serializer and value-serializer elements in all the stores -->
    <stores>
        <store>
            <name>HibernateOGM</name>
            <persistence>bdb</persistence>
            <routing>client</routing>
            <replication-factor>1</replication-factor>
            <required-reads>1</required-reads>
            <required-writes>1</required-writes>
            <key-serializer>
                <type>json</type>
            <schema-info>{"id":"string","table":"string"}</schema-info>
            </key-serializer>
            <value-serializer>
                <type>identity</type>
            </value-serializer>
            <retention-days>1</retention-days>
        </store>
        <store>
            <name>HibernateOGM-Association</name>
            <persistence>bdb</persistence>
            <routing>client</routing>
            <replication-factor>1</replication-factor>
            <required-reads>1</required-reads>
            <required-writes>1</required-writes>
            <key-serializer>
                <type>identity</type>
            </key-serializer>
            <value-serializer>
                <type>identity</type>
            </value-serializer>
            <retention-days>1</retention-days>
        </store>
        <store>
            <name>HibernateOGM-Sequence</name>
            <persistence>bdb</persistence>
            <routing>client</routing>
            <replication-factor>1</replication-factor>
            <required-reads>1</required-reads>
            <required-writes>1</required-writes>
            <key-serializer>
                <type>identity</type>
            </key-serializer>
            <value-serializer>
                <type>json</type>
                <schema-info>{"nextSequence":"int32"}</schema-info>
            </value-serializer>
            <retention-days>1</retention-days>
        </store>
    </stores>

## Type `mvn clean install`

Once the installation and configuration are done, it's time to run the integration test. It should pass all the tests with one modification on `hibernate.properties`. 

Please set `hibernate.ogm.datastore.provider_debug_location` property to `path/to/your/voldemort-config/lives`. If you modify one of the supplied sample configuration files, then it would be `VOLDEMORT_HOME/config/test_config1`, `VOLDEMORT_HOME/config/test_config2`, `VOLDEMORT_HOME/config/single_node_cluster` or something similar.

## Voldemort on `hibernate.properties`

Please check the corresponding elements on `stores.xml` when needed.

There are some properties that can be configured through `hibernate.properties` file. Here are the properties.

* `hibernate.ogm.datastore.voldemort_association_store` Store name for association. The default value is " HibernateOGM-Association".
* `hibernate.ogm.datastore.voldemort_store` Store name for entity. The default value is "HibernateOGM".
* `hibernate.ogm.datastore.voldemort_sequence_store` Store name for sequence. The default value is "HibernateOGM-Sequence".
* `hibernate.ogm.datastore.voldemort_flush_sequence_to_db` whether to flush sequences to the database or not. The default value is true. **Need to confirm with the experts about this**.
* `hibernate.ogm.datastore.voldemort_max_tries` Max number of tries when there is a conflict. The default value is 3.
* `hibernate.ogm.datastore.voldemort_update_action` Custom object which is an instance of `org.hibernate.ogm.datastore.voldemort.impl.VoldemortUpdateAction` to customize how to put data. The default value is null.