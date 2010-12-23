/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.ogm.test.associations.collection.unidirectional;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.test.simpleentity.OgmTestCase;

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
		sf.setDescription( "Snowflake 2" );
		session.save( sf2 );
		Cloud cloud = new Cloud();
		cloud.setLength( 23 );
		cloud.getProducedSnowFlakes().add( sf );
		cloud.getProducedSnowFlakes().add( sf2 );
		session.persist( cloud );
		transaction.commit();

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

		session.clear();

		transaction = session.beginTransaction();
		cloud = (Cloud) session.get( Cloud.class, cloud.getId() );
		assertNotNull( cloud.getProducedSnowFlakes() );
		assertEquals( 0, cloud.getProducedSnowFlakes().size() );
		session.delete( cloud );
		transaction.commit();

		session.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Cloud.class,
				SnowFlake.class
		};
	}
}
