/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.test.associations.storageconfiguration;

import static org.fest.assertions.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.ogm.cfg.DocumentStoreProperties;
import org.hibernate.ogm.options.generic.document.AssociationStorageType;
import org.hibernate.ogm.options.navigation.document.DocumentStoreGlobalContext;
import org.hibernate.ogm.test.associations.collection.unidirectional.Cloud;
import org.hibernate.ogm.test.associations.collection.unidirectional.SnowFlake;
import org.hibernate.ogm.test.utils.GridDialectType;
import org.hibernate.ogm.test.utils.SkipByGridDialect;
import org.hibernate.ogm.test.utils.TestHelper;
import org.junit.After;
import org.junit.Test;

/**
 * Test for configuring the association storage mode via {@link Configuration}.
 *
 * @author Gunnar Morling
 */
@SkipByGridDialect(
		value = { GridDialectType.EHCACHE, GridDialectType.HASHMAP, GridDialectType.INFINISPAN, GridDialectType.NEO4J },
		comment = "Only the document stores CouchDB and MongoDB support the configuration of specific association storage strategies"
)
public class AssociationStorageConfiguredViaPropertyTest extends AssociationStorageTestBase {

	private Cloud cloud;

	@Override
	protected void configure(Configuration cfg) {
		cfg.getProperties().put( DocumentStoreProperties.ASSOCIATIONS_STORE, AssociationStorageType.ASSOCIATION_DOCUMENT );
	}

	@Test
	public void associationStorageSetToAssociationDocumentViaProperty() throws Exception {
		setupSessionFactory();
		createCloudWithTwoProducedSnowflakes();

		assertThat( associationDocumentCount() ).isEqualTo( 1 );
		assertThat( inEntityAssociationCount() ).isEqualTo( 0 );
	}

	@Test
	public void associationStorageSetViaApiTakesPrecedenceOverProperty() throws Exception {
		( (DocumentStoreGlobalContext<?, ?>) TestHelper.configureDatastore( configuration ) )
			.associationStorage( AssociationStorageType.IN_ENTITY );

		setupSessionFactory();
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

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Cloud.class,
				SnowFlake.class
		};
	}
}
