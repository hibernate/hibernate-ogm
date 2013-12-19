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
package org.hibernate.ogm.test.couchdb.associations;

import static org.fest.assertions.Assertions.assertThat;

import java.lang.annotation.ElementType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.datastore.couchdb.CouchDB;
import org.hibernate.ogm.options.couchdb.AssociationStorageType;
import org.hibernate.ogm.test.associations.collection.unidirectional.SnowFlake;
import org.junit.After;
import org.junit.Test;

/**
 * Test for configuring the different association storage modes via annotations.
 *
 * @author Gunnar Morling
 */
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
		configuration.configureOptionsFor( CouchDB.class )
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

		assertThat( testHelper.getNumberOfEntities( sessions ) ).isEqualTo( 0 );
		assertThat( testHelper.getNumberOfAssociations( sessions ) ).isEqualTo( 0 );

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
