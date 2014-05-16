/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.associations.storageconfiguration;

import static org.fest.assertions.Assertions.assertThat;

import java.lang.annotation.ElementType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.backendtck.associations.collection.unidirectional.Cloud;
import org.hibernate.ogm.backendtck.associations.collection.unidirectional.SnowFlake;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.document.options.navigation.DocumentStoreGlobalContext;
import org.hibernate.ogm.utils.GridDialectType;
import org.hibernate.ogm.utils.SkipByGridDialect;
import org.hibernate.ogm.utils.TestHelper;
import org.junit.After;
import org.junit.Test;

/**
 * Test for configuring the different association storage modes via the option API.
 *
 * @author Gunnar Morling
 */
@SkipByGridDialect(
		value = { GridDialectType.EHCACHE, GridDialectType.HASHMAP, GridDialectType.INFINISPAN, GridDialectType.NEO4J },
		comment = "Only the document stores CouchDB and MongoDB support the configuration of specific association storage strategies"
)
public class AssociationStorageConfiguredProgrammaticallyTest extends AssociationStorageTestBase {

	private Cloud cloud;

	@Test
	public void associationStorageSetToAssociationDocumentOnGlobalLevel() throws Exception {
		( (DocumentStoreGlobalContext<?, ?>) TestHelper.configureDatastore( configuration ) )
			.associationStorage( AssociationStorageType.ASSOCIATION_DOCUMENT );

		setupSessionFactory();
		createCloudWithTwoProducedSnowflakes();

		assertThat( associationDocumentCount() ).isEqualTo( 1 );
		assertThat( inEntityAssociationCount() ).isEqualTo( 0 );
	}

	@Test
	public void associationStorageSetToInEntityOnGlobalLevel() throws Exception {
		( (DocumentStoreGlobalContext<?, ?>) TestHelper.configureDatastore( configuration ) )
			.associationStorage( AssociationStorageType.IN_ENTITY );

		setupSessionFactory();
		createCloudWithTwoProducedSnowflakes();

		assertThat( associationDocumentCount() ).isEqualTo( 0 );
		assertThat( inEntityAssociationCount() ).isEqualTo( 1 );
	}

	@Test
	public void associationStorageSetToAssociationDocumentOnEntityLevel() throws Exception {
		( (DocumentStoreGlobalContext<?, ?>) TestHelper.configureDatastore( configuration ) )
			.entity( Cloud.class )
				.associationStorage( AssociationStorageType.ASSOCIATION_DOCUMENT );

		setupSessionFactory();
		createCloudWithTwoProducedSnowflakes();

		assertThat( associationDocumentCount() ).isEqualTo( 1 );
		assertThat( inEntityAssociationCount() ).isEqualTo( 0 );
	}

	@Test
	public void associationStorageSetToInEntityOnEntityLevel() throws Exception {
		( (DocumentStoreGlobalContext<?, ?>) TestHelper.configureDatastore( configuration ) )
			.entity( Cloud.class )
				.associationStorage( AssociationStorageType.IN_ENTITY );

		setupSessionFactory();
		createCloudWithTwoProducedSnowflakes();

		assertThat( associationDocumentCount() ).isEqualTo( 0 );
		assertThat( inEntityAssociationCount() ).isEqualTo( 1 );
	}

	@Test
	public void associationStorageSetOnPropertyLevel() throws Exception {
		( (DocumentStoreGlobalContext<?, ?>) TestHelper.configureDatastore( configuration ) )
			.entity( Cloud.class )
				.property( "producedSnowFlakes", ElementType.METHOD )
					.associationStorage( AssociationStorageType.ASSOCIATION_DOCUMENT )
				.property( "backupSnowFlakes", ElementType.METHOD )
					.associationStorage( AssociationStorageType.IN_ENTITY );

		setupSessionFactory();
		createCloudWithTwoProducedAndOneBackupSnowflake();

		assertThat( associationDocumentCount() ).isEqualTo( 1 );
		assertThat( inEntityAssociationCount() ).isEqualTo( 1 );
	}

	@Test
	public void associationStorageSetOnPropertyLevelTakesPrecedenceOverEntityLevel() throws Exception {
		( (DocumentStoreGlobalContext<?, ?>) TestHelper.configureDatastore( configuration ) )
		.entity( Cloud.class )
			.associationStorage( AssociationStorageType.IN_ENTITY )
			.property( "backupSnowFlakes", ElementType.METHOD )
				.associationStorage( AssociationStorageType.ASSOCIATION_DOCUMENT );

		setupSessionFactory();
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
		Session session = sessions.openSession();
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
		session.close();

		assertThat( TestHelper.getNumberOfEntities( sessions ) ).isEqualTo( 0 );
		assertThat( TestHelper.getNumberOfAssociations( sessions ) ).isEqualTo( 0 );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Cloud.class,
				SnowFlake.class
		};
	}
}
