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
import org.hibernate.ogm.backendtck.associations.collection.unidirectional.SnowFlake;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.document.options.navigation.DocumentStoreGlobalContext;
import org.hibernate.ogm.utils.GridDialectType;
import org.hibernate.ogm.utils.SkipByGridDialect;
import org.hibernate.ogm.utils.TestHelper;
import org.junit.After;
import org.junit.Test;

/**
 * Test for configuring the different association storage modes via annotations.
 *
 * @author Gunnar Morling
 */
@SkipByGridDialect(
		value = { GridDialectType.EHCACHE, GridDialectType.HASHMAP, GridDialectType.INFINISPAN, GridDialectType.NEO4J },
		comment = "Only the document stores CouchDB and MongoDB support the configuration of specific association storage strategies"
)
public class AssociationStorageConfiguredViaAnnotationsTest extends AssociationStorageTestBase {

	private AnnotatedCloud cloud;
	private PolarCloud polarCloud;

	@Test
	public void associationStorageSetToCollectionOnEntityLevel() throws Exception {
		setupSessionFactory();
		createCloudWithTwoProducedSnowflakes();

		assertThat( associationDocumentCount() ).isEqualTo( 0 );
		assertThat( inEntityAssociationCount() ).isEqualTo( 1 );
	}

	@Test
	public void associationStorageSetOnPropertyLevelTakesPrecedenceOverEntityLevel() throws Exception {
		setupSessionFactory();
		createCloudWithTwoProducedAndOneBackupSnowflake();

		assertThat( associationDocumentCount() ).isEqualTo( 1 );
		assertThat( inEntityAssociationCount() ).isEqualTo( 1 );
	}

	@Test
	public void associationStorageSetOnPropertyLevelViaApiTakesPrecedenceOverAnnotation() throws Exception {
		( (DocumentStoreGlobalContext<?, ?>) TestHelper.configureDatastore( configuration ) )
			.entity( AnnotatedCloud.class )
				.property( "backupSnowFlakes", ElementType.METHOD )
					.associationStorage( AssociationStorageType.IN_ENTITY );

		setupSessionFactory();

		createCloudWithTwoProducedAndOneBackupSnowflake();

		assertThat( associationDocumentCount() ).isEqualTo( 0 );
		assertThat( inEntityAssociationCount() ).isEqualTo( 2 );
	}

	@Test
	public void associationStorageSetOnSubClass() throws Exception {
		setupSessionFactory();
		createPolarCloudWithTwoProducedAndOneBackupSnowflake();

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

	private void createPolarCloudWithTwoProducedAndOneBackupSnowflake() {
		polarCloud = newPolarCloud()
				.withLength( 23 )
				.withProducedSnowflakes( "Snowflake1", "Snowflake2" )
				.withBackupSnowflakes( "Snowflake3" )
				.createAndSave();
	}

	private CloudBuilder<AnnotatedCloud> newCloud() {
		return new CloudBuilder<AnnotatedCloud>( false );
	}

	private CloudBuilder<PolarCloud> newPolarCloud() {
		return new CloudBuilder<PolarCloud>( true );
	}

	private class CloudBuilder<T> {

		private final boolean polar;
		private int length;
		private final List<String> producedSnowflakes = new ArrayList<String>();
		private final List<String> backupSnowflakes = new ArrayList<String>();

		private CloudBuilder(boolean polar) {
			this.polar = polar;
		}

		public CloudBuilder<T> withLength(int length) {
			this.length = length;
			return this;
		}

		public CloudBuilder<T> withProducedSnowflakes(String... descriptions) {
			this.producedSnowflakes.addAll( Arrays.asList( descriptions ) );
			return this;
		}

		public CloudBuilder<T> withBackupSnowflakes(String... descriptions) {
			this.backupSnowflakes.addAll( Arrays.asList( descriptions ) );
			return this;
		}

		public T createAndSave() {
			Session session = sessions.openSession();
			Transaction transaction = session.beginTransaction();

			Object cloud = null;

			if ( polar ) {
				PolarCloud polarCloud = new PolarCloud();
				polarCloud.setLength( length );

				for ( String description : producedSnowflakes ) {
					SnowFlake sf = new SnowFlake();
					sf.setDescription( description );
					session.save( sf );
					polarCloud.getProducedSnowFlakes().add( sf );
				}

				for ( String description : backupSnowflakes ) {
					SnowFlake sf = new SnowFlake();
					sf.setDescription( description );
					session.save( sf );
					polarCloud.getBackupSnowFlakes().add( sf );
				}
				cloud = polarCloud;
			}
			else {
				AnnotatedCloud annotatedCloud = new AnnotatedCloud();
				annotatedCloud.setLength( length );

				for ( String description : producedSnowflakes ) {
					SnowFlake sf = new SnowFlake();
					sf.setDescription( description );
					session.save( sf );
					annotatedCloud.getProducedSnowFlakes().add( sf );
				}

				for ( String description : backupSnowflakes ) {
					SnowFlake sf = new SnowFlake();
					sf.setDescription( description );
					session.save( sf );
					annotatedCloud.getBackupSnowFlakes().add( sf );
				}
				cloud = annotatedCloud;
			}

			session.persist( cloud );

			transaction.commit();
			session.close();

			@SuppressWarnings("unchecked")
			T result = (T) cloud;

			return result;
		}
	}

	@After
	public void removeCloudAndSnowflakes() {
		Session session = sessions.openSession();
		Transaction transaction = session.beginTransaction();

		if ( cloud != null ) {
			AnnotatedCloud cloudToDelete = (AnnotatedCloud) session.get( AnnotatedCloud.class, cloud.getId() );
			for ( SnowFlake current : cloudToDelete.getProducedSnowFlakes() ) {
				session.delete( current );
			}
			for ( SnowFlake current : cloudToDelete.getBackupSnowFlakes() ) {
				session.delete( current );
			}
			session.delete( cloudToDelete );
		}

		if ( polarCloud != null ) {
			PolarCloud cloudToDelete = (PolarCloud) session.get( PolarCloud.class, polarCloud.getId() );
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
				AnnotatedCloud.class,
				PolarCloud.class,
				SnowFlake.class
		};
	}
}
