/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.associations.storageconfiguration;

import static org.fest.assertions.Assertions.assertThat;

import java.lang.annotation.ElementType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.OgmSessionFactory;
import org.hibernate.ogm.cfg.OgmConfiguration;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.mongodb.options.AssociationDocumentStorageType;
import org.hibernate.ogm.datastore.mongodb.utils.MongoDBTestHelper;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.TestHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for configuring the different association document storage modes via the option API.
 *
 * @author Gunnar Morling
 */
public class AssociationDocumentStorageConfiguredProgrammaticallyTest extends OgmTestCase {

	private final MongoDBTestHelper testHelper = new MongoDBTestHelper();

	private OgmConfiguration configuration;
	private OgmSessionFactory sessions;

	private Cloud cloud;

	@Before
	public void setupConfiguration() {
		configuration = TestHelper.getDefaultTestConfiguration( getAnnotatedClasses() );
		configure( configuration );
	}

	protected void setupSessionFactory() {
		sessions = configuration.buildSessionFactory();
	}

	@Test
	public void associationDocumentStorageSetOnGlobalLevel() throws Exception {
		testHelper.configureDatastore( configuration )
			.associationStorage( AssociationStorageType.ASSOCIATION_DOCUMENT )
			.associationDocumentStorage( AssociationDocumentStorageType.COLLECTION_PER_ASSOCIATION );

		setupSessionFactory();
		createCloudWithTwoProducedSnowflakes();

		assertThat( testHelper.getNumberOfEmbeddedAssociations( sessions ) ).isEqualTo( 0 );
		assertThat( testHelper.getNumberOfAssociationsFromGlobalCollection( sessions ) ).isEqualTo( 0 );
		assertThat( testHelper.getNumberOfAssociationsFromDedicatedCollections( sessions ) ).isEqualTo( 1 );
	}

	@Test
	public void associationDocumentStorageSetOnEntityLevel() throws Exception {
		testHelper.configureDatastore( configuration )
			.entity( Cloud.class )
				.associationStorage( AssociationStorageType.ASSOCIATION_DOCUMENT )
				.associationDocumentStorage( AssociationDocumentStorageType.COLLECTION_PER_ASSOCIATION );

		setupSessionFactory();
		createCloudWithTwoProducedSnowflakes();

		assertThat( testHelper.getNumberOfEmbeddedAssociations( sessions ) ).isEqualTo( 0 );
		assertThat( testHelper.getNumberOfAssociationsFromGlobalCollection( sessions ) ).isEqualTo( 0 );
		assertThat( testHelper.getNumberOfAssociationsFromDedicatedCollections( sessions ) ).isEqualTo( 1 );
	}

	@Test
	public void associationDocumentStorageSetOnPropertyLevel() throws Exception {
		testHelper.configureDatastore( configuration )
			.entity( Cloud.class )
				.associationStorage( AssociationStorageType.ASSOCIATION_DOCUMENT )
				.property( "producedSnowFlakes", ElementType.METHOD )
					.associationDocumentStorage( AssociationDocumentStorageType.COLLECTION_PER_ASSOCIATION )
				.property( "backupSnowFlakes", ElementType.METHOD )
					.associationDocumentStorage( AssociationDocumentStorageType.GLOBAL_COLLECTION );

		setupSessionFactory();
		createCloudWithTwoProducedAndOneBackupSnowflake();

		assertThat( testHelper.getNumberOfEmbeddedAssociations( sessions ) ).isEqualTo( 0 );
		assertThat( testHelper.getNumberOfAssociationsFromGlobalCollection( sessions ) ).isEqualTo( 1 );
		assertThat( testHelper.getNumberOfAssociationsFromDedicatedCollections( sessions ) ).isEqualTo( 1 );
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

		sessions.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Cloud.class,
				SnowFlake.class
		};
	}
}
