/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.integration.mongodb.wildflynosql;

import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.mongodb.MongoDB;
import org.hibernate.ogm.test.integration.mongodb.MongoDBModuleMemberRegistrationScenario;
import org.hibernate.ogm.test.integration.mongodb.errorhandler.TestErrorHandler;

import org.junit.Ignore;
import org.junit.runner.RunWith;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceDescriptor;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceUnit;
import org.jboss.shrinkwrap.descriptor.api.persistence20.Properties;

/**
 * Test for the Hibernate OGM module in WildFly using MongoDB
 * and WildFly NoSql subsystem
 *
 * @author Fabio Massimo Ercoli
 */
@RunWith(Arquillian.class)
@Ignore("WildFly 14 mongo subsystem is not compatible with mongo client 4. Add back the subsystem in standalone-nosql.xml to enable the test")
public class MongoDBWildFlyNoSQLMemberRegistrationIT extends MongoDBModuleMemberRegistrationScenario {

	@Deployment
	public static Archive<?> createTestArchive() {
		return createTestArchiveFromPersistenceXml( persistenceXml() );
	}

	private static PersistenceDescriptor persistenceXml() {
		Properties<PersistenceUnit<PersistenceDescriptor>> propertiesContext = Descriptors.create( PersistenceDescriptor.class )
				.version( "2.0" )
				.createPersistenceUnit()
				.name( "primary" )
				.provider( "org.hibernate.ogm.jpa.HibernateOgmPersistence" )
				.getOrCreateProperties();
		return propertiesContext
					.createProperty().name( OgmProperties.DATASTORE_PROVIDER ).value( MongoDB.DATASTORE_PROVIDER_NAME ).up()
					.createProperty().name( OgmProperties.NATIVE_CLIENT_RESOURCE ).value( "java:jboss/mongodb/client" ).up()
					.createProperty().name( OgmProperties.CREATE_DATABASE ).value( "true" ).up()
					.createProperty().name( OgmProperties.ERROR_HANDLER ).value( TestErrorHandler.class.getName() ).up()
					.createProperty().name( "hibernate.search.default.directory_provider" ).value( "ram" ).up()
					.createProperty().name( "wildfly.jpa.hibernate.search.module" ).value( "org.hibernate.search.orm:${module-slot.org.hibernate.search.short-id}" ).up()
				.up().up();
	}
}
