/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2010-2011 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.test.associations.collection.unidirectional;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.test.simpleentity.OgmTestCase;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.test.utils.TestHelper.assertNumberOfAssociations;
import static org.hibernate.ogm.test.utils.TestHelper.assertNumberOfEntities;

/**
 * @author Emmanuel Bernard
 */
public class CollectionUnidirectionalTest extends OgmTestCase {

	public void testUnidirectionalCollection() throws Exception {
		final Session session = openSession();
		Transaction transaction = session.beginTransaction();
		SnowFlake sf = new SnowFlake();
		sf.setDescription( "Snowflake 1" );
		session.save( sf );
		SnowFlake sf2 = new SnowFlake();
		sf2.setDescription( "Snowflake 2" );
		session.save( sf2 );
		Cloud cloud = new Cloud();
		cloud.setLength( 23 );
		cloud.getProducedSnowFlakes().add( sf );
		cloud.getProducedSnowFlakes().add( sf2 );
		session.persist( cloud );
		session.flush();
		assertThat( assertNumberOfEntities( 3, sessions ) ).isTrue();
		assertThat( assertNumberOfAssociations( 1, sessions ) ).isTrue();
		transaction.commit();

		assertThat( assertNumberOfEntities( 3, sessions ) ).isTrue();
		assertThat( assertNumberOfAssociations( 1, sessions ) ).isTrue();

		session.clear();

		transaction = session.beginTransaction();
		cloud = (Cloud) session.get( Cloud.class, cloud.getId() );
		assertNotNull( cloud.getProducedSnowFlakes() );
		assertEquals( 2, cloud.getProducedSnowFlakes().size() );
		final SnowFlake removedSf = cloud.getProducedSnowFlakes().iterator().next();
		SnowFlake sf3 = new SnowFlake();
		sf3.setDescription( "Snowflake 3" );
		session.persist( sf3 );
		cloud.getProducedSnowFlakes().remove( removedSf );
		cloud.getProducedSnowFlakes().add( sf3 );
		transaction.commit();

		assertThat( assertNumberOfEntities( 4, sessions ) ).isTrue();
		assertThat( assertNumberOfAssociations( 1, sessions ) ).isTrue();

		session.clear();

		transaction = session.beginTransaction();
		cloud = (Cloud) session.get( Cloud.class, cloud.getId() );
		assertNotNull( cloud.getProducedSnowFlakes() );
		assertEquals( 2, cloud.getProducedSnowFlakes().size() );
		boolean present = false;
		for ( SnowFlake current : cloud.getProducedSnowFlakes() ) {
			if ( current.getDescription().equals( removedSf.getDescription() ) ) {
				present = true;
			}
		}
		assertFalse( "flake not removed", present );
		for ( SnowFlake current : cloud.getProducedSnowFlakes() ) {
			session.delete( current );
		}
		session.delete( session.load( SnowFlake.class, removedSf.getId() ) );
		cloud.getProducedSnowFlakes().clear();
		transaction.commit();

		assertThat( assertNumberOfEntities( 1, sessions ) ).isTrue();
		assertThat( assertNumberOfAssociations( 0, sessions ) ).isTrue();

		session.clear();

		transaction = session.beginTransaction();
		cloud = (Cloud) session.get( Cloud.class, cloud.getId() );
		assertNotNull( cloud.getProducedSnowFlakes() );
		assertEquals( 0, cloud.getProducedSnowFlakes().size() );
		session.delete( cloud );
		session.flush();
		transaction.commit();

		assertThat( assertNumberOfEntities( 0, sessions ) ).isTrue();
		assertThat( assertNumberOfAssociations( 0, sessions ) ).isTrue();
		session.close();

		checkCleanCache();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Cloud.class,
				SnowFlake.class
		};
	}
}
