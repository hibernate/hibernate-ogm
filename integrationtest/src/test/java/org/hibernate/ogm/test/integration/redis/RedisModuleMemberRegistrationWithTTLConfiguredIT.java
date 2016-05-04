/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.integration.redis;

import static org.junit.Assert.assertTrue;

import javax.inject.Inject;
import javax.persistence.EntityManagerFactory;

import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.redis.Redis;
import org.hibernate.ogm.datastore.redis.RedisJsonDialect;
import org.hibernate.ogm.datastore.redis.RedisProperties;
import org.hibernate.ogm.datastore.redis.impl.RedisDatastoreProvider;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.jpa.impl.OgmEntityManagerFactory;
import org.hibernate.ogm.test.integration.redis.model.PhoneNumber;
import org.hibernate.ogm.test.integration.redis.service.PhoneNumberService;
import org.hibernate.ogm.test.integration.testcase.ModuleMemberRegistrationScenario;
import org.hibernate.ogm.test.integration.testcase.model.Member;
import org.hibernate.ogm.test.integration.testcase.util.ModuleMemberRegistrationDeployment;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceDescriptor;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceUnit;
import org.jboss.shrinkwrap.descriptor.api.persistence20.Properties;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.lambdaworks.redis.cluster.api.sync.RedisClusterCommands;

/**
 * Test for the Hibernate OGM module in WildFly using Redis
 *
 * @author Mark Paluch
 */
@RunWith(Arquillian.class)
public class RedisModuleMemberRegistrationWithTTLConfiguredIT extends ModuleMemberRegistrationScenario {

	@Inject
	private PhoneNumberService phoneNumberService;

	@javax.persistence.PersistenceUnit
	private EntityManagerFactory entityManagerFactory;

	@Deployment
	public static Archive<?> createTestArchive() {
		return new ModuleMemberRegistrationDeployment
				.Builder( RedisModuleMemberRegistrationWithTTLConfiguredIT.class )
				.addClasses( PhoneNumber.class, PhoneNumberService.class )
				.persistenceXml( persistenceXml() )
				.manifestDependencies(
						"org.hibernate.ogm:${hibernate-ogm.module.slot} services, org.hibernate.ogm.redis:${hibernate-ogm.module.slot} services,  org.hibernate.ogm.redis.driver:${hibernate-ogm.module.slot} services"
				)
				.createDeployment();
	}

	private static PersistenceDescriptor persistenceXml() {
		Properties<PersistenceUnit<PersistenceDescriptor>> propertiesContext = Descriptors.create( PersistenceDescriptor.class )
				.version( "2.0" )
				.createPersistenceUnit()
				.name( "primary" )
				.provider( "org.hibernate.ogm.jpa.HibernateOgmPersistence" )
				.clazz( Member.class.getName() )
				.clazz( PhoneNumber.class.getName() )
				.getOrCreateProperties();

		if ( RedisTestProperties.getPassword() != null ) {
			propertiesContext.createProperty().name( OgmProperties.PASSWORD ).value( RedisTestProperties.getPassword() );
		}
		return propertiesContext
				.createProperty().name( OgmProperties.HOST ).value( RedisTestProperties.getHost() ).up()
				.createProperty().name( OgmProperties.DATASTORE_PROVIDER ).value( Redis.DATASTORE_PROVIDER_NAME ).up()
				.createProperty().name( OgmProperties.DATABASE ).value( "0" ).up()
				.createProperty().name( RedisProperties.TTL ).value( "3600" ).up()
				.createProperty().name( "hibernate.search.default.directory_provider" ).value( "ram" ).up()
				.createProperty().name( "hibernate.transaction.jta.platform" ).value( "JBossAS" ).up()
				.up().up();
	}

	@Test
	public void testTtlIsSet() throws InterruptedException {
		// given
		phoneNumberService.createPhoneNumber( "Michael", "123-456" );

		RedisDatastoreProvider provider = getProvider();
		RedisClusterCommands<String, String> connection = provider.getConnection();

		// when
		String key = "PhoneNumber:Michael";
		Boolean exists = connection.exists( key );
		String value = connection.get( key );
		Long pttl = connection.pttl( key );

		// then
		assertTrue( exists );
		assertTrue( value.length() > 10 );
		assertTrue( pttl.longValue() > 3500 );
	}


	private RedisDatastoreProvider getProvider() {

		OgmEntityManagerFactory ogmEntityManagerFactory = (OgmEntityManagerFactory) entityManagerFactory;

		DatastoreProvider provider = ogmEntityManagerFactory.getSessionFactory().getServiceRegistry().getService(
				DatastoreProvider.class
		);
		if ( !( RedisDatastoreProvider.class.isInstance( provider ) ) ) {
			throw new RuntimeException( "Not testing with RedisDatastoreProvider, cannot extract underlying instance." );
		}

		return RedisDatastoreProvider.class.cast( provider );
	}

	public static RedisJsonDialect getDialect(DatastoreProvider datastoreProvider) {
		return new RedisJsonDialect( (RedisDatastoreProvider) datastoreProvider );
	}

}
