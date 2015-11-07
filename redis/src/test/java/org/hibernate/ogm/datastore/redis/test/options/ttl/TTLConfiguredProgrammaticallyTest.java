/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.test.options.ttl;

import java.lang.annotation.ElementType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.OgmSessionFactory;
import org.hibernate.ogm.backendtck.associations.collection.unidirectional.Cloud;
import org.hibernate.ogm.backendtck.associations.collection.unidirectional.SnowFlake;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.redis.Redis;
import org.hibernate.ogm.datastore.redis.impl.RedisDatastoreProvider;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.utils.TestHelper;

import org.junit.After;
import org.junit.Test;

import com.lambdaworks.redis.RedisConnection;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Test for configuring the different association storage modes via the option API.
 *
 * @author Mark Paluch
 */
public class TTLConfiguredProgrammaticallyTest {

	protected OgmSessionFactory sessions;

	private Cloud cloud;

	private void setupSessionFactory(Map<String, Object> settings) {
		sessions = TestHelper.getDefaultTestSessionFactory( settings, Cloud.class, SnowFlake.class );
	}

	@Test
	public void ttlOnGlobalLevel() throws Exception {
		Map<String, Object> settings = new HashMap<String, Object>();

		TestHelper.configureOptionsFor( settings, Redis.class )
				.ttl( 1, TimeUnit.DAYS );

		setupSessionFactory( settings );
		createCloudWithTwoProducedSnowflakes();

		assertThat( cloudTtl() ).isGreaterThan( TimeUnit.HOURS.toMillis( 23 ) )
				.isLessThanOrEqualTo( TimeUnit.HOURS.toMillis( 24 ) );
	}

	@Test
	public void ttlOnEntityLevel() throws Exception {
		Map<String, Object> settings = new HashMap<String, Object>();

		TestHelper.configureOptionsFor( settings, Redis.class )
				.entity( Cloud.class )
				.ttl( 1, TimeUnit.DAYS );

		setupSessionFactory( settings );
		createCloudWithTwoProducedSnowflakes();

		assertThat( cloudTtl() ).isGreaterThan( TimeUnit.HOURS.toMillis( 23 ) ).isLessThanOrEqualTo(
				TimeUnit.HOURS.toMillis(
						24
				)
		);
	}

	@Test
	public void ttlOnPropertyLevel() throws Exception {
		Map<String, Object> settings = new HashMap<String, Object>();

		TestHelper.configureOptionsFor( settings, Redis.class )
				.associationStorage( AssociationStorageType.ASSOCIATION_DOCUMENT )
				.entity( Cloud.class )
				.property( "backupSnowFlakes", ElementType.METHOD )
				.ttl( 1, TimeUnit.DAYS );

		setupSessionFactory( settings );
		createCloudWithTwoProducedAndOneBackupSnowflake();

		// property-level options are applied to the type as well.
		// not sure, whether this is a good idea.
		assertThat( cloudTtl() ).isGreaterThan( TimeUnit.HOURS.toMillis( 23 ) )
				.isLessThanOrEqualTo( TimeUnit.HOURS.toMillis( 24 ) );

		assertThat( associationTtl() ).isGreaterThan( TimeUnit.HOURS.toMillis( 23 ) )
				.isLessThanOrEqualTo( TimeUnit.HOURS.toMillis( 24 ) );
	}


	private long cloudTtl() {
		return getConnection().pttl( "Cloud:" + cloud.getId() );
	}

	private long associationTtl() {
		String associationKey = getConnection()
				.keys( "Associations:joinBackupSnowflakes:*" )
				.get( 0 );
		return getConnection().pttl( associationKey );
	}

	private RedisConnection<String, String> getConnection() {
		return getProvider().getConnection();
	}

	private RedisDatastoreProvider getProvider() {
		return (RedisDatastoreProvider) ( (SessionFactoryImplementor) sessions )
				.getServiceRegistry()
				.getService( DatastoreProvider.class );
	}

	private void createCloudWithTwoProducedSnowflakes() {
		cloud = newCloud()
				.withLength( 23 )
				.withProducedSnowflakes( "Snowflake1", "Snowflake2" )
				.createAndSave();
	}

	private void createCloudWithTwoProducedAndOneBackupSnowflake() {
		cloud = newCloud()
				.withLength( 23 )
				.withProducedSnowflakes( "Snowflake1", "Snowflake2" )
				.withBackupSnowflakes( "Snowflake3" )
				.createAndSave();
	}

	private CloudBuilder newCloud() {
		return new CloudBuilder();
	}

	private class CloudBuilder {

		private int length;
		private final List<String> producedSnowflakes = new ArrayList<String>();
		private final List<String> backupSnowflakes = new ArrayList<String>();

		public CloudBuilder withLength(int length) {
			this.length = length;
			return this;
		}

		public CloudBuilder withProducedSnowflakes(String... descriptions) {
			this.producedSnowflakes.addAll( Arrays.asList( descriptions ) );
			return this;
		}

		public CloudBuilder withBackupSnowflakes(String... descriptions) {
			this.backupSnowflakes.addAll( Arrays.asList( descriptions ) );
			return this;
		}

		public Cloud createAndSave() {
			Session session = sessions.openSession();
			Transaction transaction = session.beginTransaction();

			Cloud cloud = new Cloud();
			cloud.setLength( length );

			for ( String description : producedSnowflakes ) {
				SnowFlake sf = new SnowFlake();
				sf.setDescription( description );
				session.save( sf );
				cloud.getProducedSnowFlakes().add( sf );
			}

			for ( String description : backupSnowflakes ) {
				SnowFlake sf = new SnowFlake();
				sf.setDescription( description );
				session.save( sf );
				cloud.getBackupSnowFlakes().add( sf );
			}

			session.persist( cloud );

			transaction.commit();
			session.close();

			return cloud;
		}
	}

	@After
	public void removeCloudAndSnowflakes() {
		Session session = sessions.openSession();
		Transaction transaction = session.beginTransaction();

		if ( cloud != null ) {
			Cloud cloudToDelete = session.get( Cloud.class, cloud.getId() );
			for ( SnowFlake current : cloudToDelete.getProducedSnowFlakes() ) {
				session.delete( current );
			}
			for ( SnowFlake current : cloudToDelete.getBackupSnowFlakes() ) {
				session.delete( current );
			}
			session.delete( cloudToDelete );
		}

		transaction.commit();
		session.close();

		assertThat( TestHelper.getNumberOfEntities( sessions ) ).isEqualTo( 0 );
		assertThat( TestHelper.getNumberOfAssociations( sessions ) ).isEqualTo( 0 );
	}
}
