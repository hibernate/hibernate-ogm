/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.associations.collection.unidirectional;
import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.utils.TestHelper.getNumberOfAssociations;
import static org.hibernate.ogm.utils.TestHelper.getNumberOfEntities;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.utils.OgmTestCase;
import org.junit.Test;

/**
 * @author Emmanuel Bernard
 */
public class CollectionUnidirectionalTest extends OgmTestCase {

	@Test
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
		assertThat( getNumberOfEntities( sessions ) ).isEqualTo( 3 );
		assertThat( getNumberOfAssociations( sessions ) ).isEqualTo( 1 );
		transaction.commit();

		assertThat( getNumberOfEntities( sessions ) ).isEqualTo( 3 );
		assertThat( getNumberOfAssociations( sessions ) ).isEqualTo( 1 );

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

		assertThat( getNumberOfEntities( sessions ) ).isEqualTo( 4 );
		assertThat( getNumberOfAssociations( sessions ) ).isEqualTo( 1 );

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

		assertThat( getNumberOfEntities( sessions ) ).isEqualTo( 1 );
		assertThat( getNumberOfAssociations( sessions ) ).isEqualTo( 0 );

		session.clear();

		transaction = session.beginTransaction();
		cloud = (Cloud) session.get( Cloud.class, cloud.getId() );
		assertNotNull( cloud.getProducedSnowFlakes() );
		assertEquals( 0, cloud.getProducedSnowFlakes().size() );
		session.delete( cloud );
		session.flush();
		transaction.commit();

		assertThat( getNumberOfEntities( sessions ) ).isEqualTo( 0 );
		assertThat( getNumberOfAssociations( sessions ) ).isEqualTo( 0 );
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
