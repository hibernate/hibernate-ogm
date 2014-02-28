/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.datastore.mongodb.test.associations.storageconfiguration;

import static org.fest.assertions.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.datastore.mongodb.utils.MongoDBTestHelper;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.TestHelper;
import org.junit.After;
import org.junit.Test;

/**
 * Test for configuring the different association document storage modes via annotations.
 *
 * @author Gunnar Morling
 */
public class AssociationDocumentStorageConfiguredViaAnnotationsTest extends OgmTestCase {

	private final MongoDBTestHelper testHelper = new MongoDBTestHelper();

	private AnnotatedCloud cloud;

	@Test
	public void associationDocumentStorageSetToCollectionPerTypeOnEntityLevel() throws Exception {
		createCloudWithTwoProducedSnowflakes();

		assertThat( testHelper.getNumberOfEmbeddedAssociations( sessions ) ).isEqualTo( 0 );
		assertThat( testHelper.getNumberOfAssociationsFromGlobalCollection( sessions ) ).isEqualTo( 0 );
		assertThat( testHelper.getNumberOfAssociationsFromDedicatedCollections( sessions ) ).isEqualTo( 1 );
	}

	@Test
	public void associationDocumentStorageSetOnPropertyLevelTakesPrecedenceOverEntityLevel() throws Exception {
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

		private CloudBuilder() {
		}

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

		public AnnotatedCloud createAndSave() {
			Session session = sessions.openSession();
			Transaction transaction = session.beginTransaction();

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

			session.persist( annotatedCloud );

			transaction.commit();
			session.close();

			return annotatedCloud;
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

		transaction.commit();
		session.close();

		assertThat( TestHelper.getNumberOfEntities( sessions ) ).isEqualTo( 0 );
		assertThat( TestHelper.getNumberOfAssociations( sessions ) ).isEqualTo( 0 );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				AnnotatedCloud.class,
				SnowFlake.class
		};
	}
}
