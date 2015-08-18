/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.integration.jboss;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;

import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.redis.Redis;
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
 * Test for the Hibernate OGM module in WildFly using Redis
 *
 * @author Mark Paluch
 */
@RunWith(Arquillian.class)
public class RedisModuleMemberRegistrationIT extends ModuleMemberRegistrationScenario {

	@Inject
	private PhoneNumberService phoneNumberService;

	@Deployment
	public static Archive<?> createTestArchive() {
		return new ModuleMemberRegistrationDeployment
				.Builder( RedisModuleMemberRegistrationIT.class )
				.addClasses( PhoneNumber.class, PhoneNumberService.class )
				.persistenceXml( persistenceXml() )
				.manifestDependencies(
						"org.hibernate.ogm services, org.hibernate.ogm.redis services, org.hibernate.search.orm:${hibernate-search.module.slot} services"
				)
				.createDeployment();
	}

	private static PersistenceDescriptor persistenceXml() {
		String host = System.getenv( "REDIS_HOSTNAME" );
		String password = System.getenv( "REDIS_PASSWORD" );

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
		if ( isNotNull( password ) ) {
			propertiesContext.createProperty().name( OgmProperties.PASSWORD ).value( password );
		}
		return propertiesContext
				.createProperty().name( OgmProperties.DATASTORE_PROVIDER ).value( Redis.DATASTORE_PROVIDER_NAME ).up()
				.createProperty().name( OgmProperties.DATABASE ).value( "0" ).up()
				.createProperty().name( "hibernate.search.default.directory_provider" ).value( "ram" ).up()
				.up().up();
	}

	private static boolean isNotNull(String value) {
		return value != null && value.length() > 0 && !"null".equals( value );
	}

	@Test
	public void shouldPersistAndFindEntityUsingId() {
		phoneNumberService.createPhoneNumber( "Bob", "123-456" );
		phoneNumberService.createPhoneNumber( "Mortimer", "789-123" );

		assertEquals( "123-456", phoneNumberService.getPhoneNumber( "Bob" ).getValue() );
		assertEquals( "789-123", phoneNumberService.getPhoneNumber( "Mortimer" ).getValue() );

		phoneNumberService.deletePhoneNumber( "Bob" );
		phoneNumberService.deletePhoneNumber( "Mortimer" );
	}

}
