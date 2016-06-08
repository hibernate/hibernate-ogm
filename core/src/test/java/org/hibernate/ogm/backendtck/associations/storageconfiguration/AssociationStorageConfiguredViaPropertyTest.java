/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.associations.storageconfiguration;

import static org.fest.assertions.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.ogm.backendtck.associations.collection.unidirectional.Cloud;
import org.hibernate.ogm.backendtck.associations.collection.unidirectional.SnowFlake;
import org.hibernate.ogm.datastore.document.cfg.DocumentStoreProperties;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.utils.GridDialectType;
import org.hibernate.ogm.utils.SkipByGridDialect;
import org.hibernate.ogm.utils.TestHelper;

import org.junit.After;
import org.junit.Test;

/**
 * Test for configuring the association storage mode via {@link Configuration}.
 *
 * @author Gunnar Morling
 */
@SkipByGridDialect(
		value = { GridDialectType.EHCACHE, GridDialectType.HASHMAP, GridDialectType.INFINISPAN, GridDialectType.NEO4J, GridDialectType.NEO4J_REMOTE, GridDialectType.CASSANDRA },
		comment = "Only the document stores CouchDB and MongoDB support the configuration of specific association storage strategies"
)
public class AssociationStorageConfiguredViaPropertyTest extends AssociationStorageTestBase {

	private Cloud cloud;

	@Test
	public void associationStorageSetToAssociationDocumentViaProperty() throws Exception {
		setupSessionFactory();
		createCloudWithTwoProducedSnowflakes();

		assertThat( associationDocumentCount() ).isEqualTo( 1 );
		assertThat( inEntityAssociationCount() ).isEqualTo( 0 );
	}

	@Test
	@SkipByGridDialect(
			value = { GridDialectType.REDIS_HASH },
			comment = "Only Redis JSON supports in-entity association storage"
	)

	public void associationStorageSetViaApiTakesPrecedenceOverProperty() throws Exception {
		Map<String, Object> settings = new HashMap<String, Object>();

		TestHelper.configureOptionsFor( settings, getDocumentDatastoreConfiguration() )
			.associationStorage( AssociationStorageType.IN_ENTITY );

		sessions = TestHelper.getDefaultTestSessionFactory( settings, Cloud.class, SnowFlake.class );

		createCloudWithTwoProducedSnowflakes();

		assertThat( associationDocumentCount() ).isEqualTo( 0 );
		assertThat( inEntityAssociationCount() ).isEqualTo( 1 );
	}

	private void createCloudWithTwoProducedSnowflakes() {
		cloud = newCloud()
				.withLength( 23 )
				.withProducedSnowflakes( "Snowflake1", "Snowflake2" )
				.createAndSave();
	}

	private CloudBuilder newCloud() {
		return new CloudBuilder();
	}

	private class CloudBuilder {

		private int length;
		private final List<String> producedSnowflakes = new ArrayList<String>();

		public CloudBuilder withLength(int length) {
			this.length = length;
			return this;
		}

		public CloudBuilder withProducedSnowflakes(String... descriptions) {
			this.producedSnowflakes.addAll( Arrays.asList( descriptions ) );
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

	private void setupSessionFactory() {
		sessions = TestHelper.getDefaultTestSessionFactory(
				Collections.<String, Object>singletonMap( DocumentStoreProperties.ASSOCIATIONS_STORE, AssociationStorageType.ASSOCIATION_DOCUMENT ),
				Cloud.class, SnowFlake.class
		);
	}
}
