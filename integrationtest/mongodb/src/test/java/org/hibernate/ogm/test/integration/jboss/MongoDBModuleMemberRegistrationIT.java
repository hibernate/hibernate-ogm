/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.integration.jboss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.inject.Inject;

import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.mongodb.MongoDB;
import org.hibernate.ogm.test.integration.jboss.model.Member;
import org.hibernate.ogm.test.integration.jboss.model.PhoneNumber;
import org.hibernate.ogm.test.integration.jboss.service.PhoneNumberService;
import org.hibernate.ogm.test.integration.jboss.util.ModuleMemberRegistrationDeployment;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceDescriptor;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceUnit;
import org.jboss.shrinkwrap.descriptor.api.persistence20.Properties;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test for the Hibernate OGM module in WildFly using MongoDB
 *
 * @author Guillaume Scheibel &lt;guillaume.scheibel@gmail.com&gt;
 */
@RunWith(Arquillian.class)
public class MongoDBModuleMemberRegistrationIT extends ModuleMemberRegistrationScenario {

	@Inject
	private PhoneNumberService phoneNumberService;

	@Deployment
	public static Archive<?> createTestArchive() {
		return new ModuleMemberRegistrationDeployment
				.Builder( MongoDBModuleMemberRegistrationIT.class )
				.addClasses( PhoneNumber.class, PhoneNumberService.class )
				.persistenceXml( persistenceXml() )
				.manifestDependencies( "org.hibernate:ogm services, org.hibernate.ogm.mongodb services" )
				.createDeployment();
	}

	private static PersistenceDescriptor persistenceXml() {
		String host = System.getenv( "MONGODB_HOSTNAME" );
		String port = System.getenv( "MONGODB_PORT" );
		String username = System.getenv( "MONGODB_USERNAME" );
		String password = System.getenv( "MONGODB_PASSWORD" );

		Properties<PersistenceUnit<PersistenceDescriptor>> propertiesContext = Descriptors.create( PersistenceDescriptor.class )
				.version( "2.0" )
				.createPersistenceUnit()
				.name( "primary" )
				.provider( "org.hibernate.ogm.jpa.HibernateOgmPersistence" )
				.clazz( Member.class.getName() )
				.clazz( PhoneNumber.class.getName() )
				.getOrCreateProperties();
		if ( isNotNull( host ) ) {
			propertiesContext.createProperty().name( OgmProperties.HOST ).value( host );
		}
		if ( isNotNull( port ) ) {
			propertiesContext.createProperty().name( OgmProperties.PORT ).value( port );
		}
		if ( isNotNull( username ) ) {
			propertiesContext.createProperty().name( OgmProperties.USERNAME ).value( username );
		}
		if ( isNotNull( password ) ) {
			propertiesContext.createProperty().name( OgmProperties.PASSWORD ).value( password );
		}
		return propertiesContext
					.createProperty().name( OgmProperties.DATASTORE_PROVIDER ).value( MongoDB.DATASTORE_PROVIDER_NAME ).up()
					.createProperty().name( OgmProperties.DATABASE ).value( "ogm_test_database" ).up()
					.createProperty().name( "hibernate.search.default.directory_provider" ).value( "ram" ).up()
				.up().up();
	}

	private static boolean isNotNull(String mongoHostName) {
		return mongoHostName != null && mongoHostName.length() > 0 && ! "null".equals( mongoHostName );
	}

	@Test
	public void shouldFindPersistedMemberByIdWithNativeQuery() throws Exception {
		Member newMember = memberRegistration.getNewMember();
		newMember.setName( "Peter O'Tall" );
		memberRegistration.register();

		String nativeQuery = "{ _id: " + newMember.getId() + " }";
		Member found = memberRegistration.findWithNativeQuery( nativeQuery );

		assertNotNull( "Expected at least one result using a native query", found );
		assertEquals( "Native query hasn't found a new member", newMember.getName(), found.getName() );
	}

	@Test
	public void shouldPersistAndFindEntityUsingObjectId() {
		phoneNumberService.createPhoneNumber( "Bob", "123-456" );
		phoneNumberService.createPhoneNumber( "Mortimer", "789-123" );

		assertEquals( "789-123", phoneNumberService.getPhoneNumber( "Mortimer" ).getValue() );
	}
}
