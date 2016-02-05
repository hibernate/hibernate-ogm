/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.associations.storageconfiguration;

import java.lang.annotation.ElementType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.backendtck.associations.collection.unidirectional.Cloud;
import org.hibernate.ogm.backendtck.associations.collection.unidirectional.SnowFlake;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.utils.GridDialectType;
import org.hibernate.ogm.utils.SkipByGridDialect;
import org.hibernate.ogm.utils.TestHelper;

import org.junit.After;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Test for configuring the different association storage modes via the option API.
 *
 * @author Gunnar Morling
 */
@SkipByGridDialect(
		value = { GridDialectType.EHCACHE, GridDialectType.HASHMAP, GridDialectType.INFINISPAN, GridDialectType.IGNITE, GridDialectType.INFINISPAN_REMOTE, GridDialectType.NEO4J_EMBEDDED, GridDialectType.NEO4J_REMOTE, GridDialectType.CASSANDRA },
		comment = "Only the document stores CouchDB and MongoDB support the configuration of specific association storage strategies"
)
public class AssociationStorageConfiguredProgrammaticallyTest extends AssociationStorageTestBase {

	private Cloud cloud;

	@Test
	public void associationStorageSetToAssociationDocumentOnGlobalLevel() throws Exception {
		Map<String, Object> settings = new HashMap<String, Object>();

		TestHelper.configureOptionsFor( settings, getDocumentDatastoreConfiguration() )
			.associationStorage( AssociationStorageType.ASSOCIATION_DOCUMENT );

		setupSessionFactory( settings );

		createCloudWithTwoProducedSnowflakes();

		assertThat( associationDocumentCount() ).isEqualTo( 1 );
		assertThat( inEntityAssociationCount() ).isEqualTo( 0 );
	}

	@Test
	@SkipByGridDialect(
			value = { GridDialectType.REDIS_HASH },
			comment = "Only Redis JSON supports in-entity association storage"
	)
	public void associationStorageSetToInEntityOnGlobalLevel() throws Exception {
		Map<String, Object> settings = new HashMap<String, Object>();

		TestHelper.configureOptionsFor( settings, getDocumentDatastoreConfiguration() )
			.associationStorage( AssociationStorageType.IN_ENTITY );

		setupSessionFactory( settings );

		createCloudWithTwoProducedSnowflakes();

		assertThat( associationDocumentCount() ).isEqualTo( 0 );
		assertThat( inEntityAssociationCount() ).isEqualTo( 1 );
	}

	@Test
	public void associationStorageSetToAssociationDocumentOnEntityLevel() throws Exception {
		Map<String, Object> settings = new HashMap<String, Object>();

		TestHelper.configureOptionsFor( settings, getDocumentDatastoreConfiguration() )
			.entity( Cloud.class )
				.associationStorage( AssociationStorageType.ASSOCIATION_DOCUMENT );

		setupSessionFactory( settings );

		createCloudWithTwoProducedSnowflakes();

		assertThat( associationDocumentCount() ).isEqualTo( 1 );
		assertThat( inEntityAssociationCount() ).isEqualTo( 0 );
	}

	@Test
	@SkipByGridDialect(
			value = { GridDialectType.REDIS_HASH },
			comment = "Only Redis JSON supports in-entity association storage"
	)
	public void associationStorageSetToInEntityOnEntityLevel() throws Exception {
		Map<String, Object> settings = new HashMap<String, Object>();

		TestHelper.configureOptionsFor( settings, getDocumentDatastoreConfiguration() )
			.entity( Cloud.class )
				.associationStorage( AssociationStorageType.IN_ENTITY );

		setupSessionFactory( settings );

		createCloudWithTwoProducedSnowflakes();

		assertThat( associationDocumentCount() ).isEqualTo( 0 );
		assertThat( inEntityAssociationCount() ).isEqualTo( 1 );
	}

	@Test
	@SkipByGridDialect(
			value = { GridDialectType.REDIS_HASH },
			comment = "Only Redis JSON supports in-entity association storage"
	)
	public void associationStorageSetOnPropertyLevel() throws Exception {
		Map<String, Object> settings = new HashMap<String, Object>();

		TestHelper.configureOptionsFor( settings, getDocumentDatastoreConfiguration() )
			.entity( Cloud.class )
				.property( "producedSnowFlakes", ElementType.METHOD )
					.associationStorage( AssociationStorageType.ASSOCIATION_DOCUMENT )
				.property( "backupSnowFlakes", ElementType.METHOD )
					.associationStorage( AssociationStorageType.IN_ENTITY );

		setupSessionFactory( settings );

		createCloudWithTwoProducedAndOneBackupSnowflake();

		assertThat( associationDocumentCount() ).isEqualTo( 1 );
		assertThat( inEntityAssociationCount() ).isEqualTo( 1 );
	}

	@Test
	@SkipByGridDialect(
			value = { GridDialectType.REDIS_HASH },
			comment = "Only Redis JSON supports in-entity association storage"
	)

	public void associationStorageSetOnPropertyLevelTakesPrecedenceOverEntityLevel() throws Exception {
		Map<String, Object> settings = new HashMap<String, Object>();

		TestHelper.configureOptionsFor( settings, getDocumentDatastoreConfiguration() )
			.entity( Cloud.class )
			.associationStorage( AssociationStorageType.IN_ENTITY )
				.property( "backupSnowFlakes", ElementType.METHOD )
					.associationStorage( AssociationStorageType.ASSOCIATION_DOCUMENT );

		setupSessionFactory( settings );

		createCloudWithTwoProducedAndOneBackupSnowflake();

		assertThat( associationDocumentCount() ).isEqualTo( 1 );
		assertThat( inEntityAssociationCount() ).isEqualTo( 1 );
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
		if ( sessions != null ) {
			try ( Session session = sessions.openSession() ) {
				Transaction transaction = session.beginTransaction();

				if ( cloud != null ) {
					Cloud cloudToDelete = (Cloud) session.get( Cloud.class, cloud.getId() );
					for ( SnowFlake current : cloudToDelete.getProducedSnowFlakes() ) {
						session.delete( current );
					}
					for ( SnowFlake current : cloudToDelete.getBackupSnowFlakes() ) {
						session.delete( current );
					}
					session.delete( cloudToDelete );
				}

				transaction.commit();
			}

			assertThat( TestHelper.getNumberOfEntities( sessions ) ).isEqualTo( 0 );
			assertThat( TestHelper.getNumberOfAssociations( sessions ) ).isEqualTo( 0 );
		}
	}

	private void setupSessionFactory(Map<String, Object> settings) {
		sessions = TestHelper.getDefaultTestSessionFactory( settings, Cloud.class, SnowFlake.class );
	}
}
