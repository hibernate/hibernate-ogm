/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.utils.test.associations;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.datastore.ignite.impl.IgniteDatastoreProvider;
import org.hibernate.ogm.datastore.ignite.logging.impl.Log;
import org.hibernate.ogm.datastore.ignite.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.ignite.utils.IgniteTestHelper;
import org.hibernate.ogm.utils.OgmTestCase;

import org.junit.Test;

import org.apache.ignite.binary.BinaryObject;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.utils.TestHelper.getNumberOfAssociations;
import static org.hibernate.ogm.utils.TestHelper.getNumberOfEntities;
import static org.junit.Assert.assertNotNull;

/**
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
public class ReadWriteThroughOneToManyTest extends OgmTestCase {
	private Log logger = LoggerFactory.getLogger();

	@Test
	public void testUnidirectionalOneToMany() throws Exception {
		final Session session = openSession();
		Transaction transaction = session.beginTransaction();
		CacheStoreJUG sourceJUG = new CacheStoreJUG( "summer_camp" );
		sourceJUG.setName( "CacheStoreJUG Summer Camp" );
		session.persist( sourceJUG );
		CacheStoreMember sourceEmmanuel = new CacheStoreMember( "emmanuel" );
		sourceEmmanuel.setName( "Emmanuel Bernard" );
		sourceEmmanuel.setMemberOf( sourceJUG );
		CacheStoreMember sourceJerome = new CacheStoreMember( "jerome" );
		sourceJerome.setName( "Jerome" );
		sourceJerome.setMemberOf( sourceJUG );
		session.persist( sourceEmmanuel );
		session.persist( sourceJerome );
		List<CacheStoreMember> members = new LinkedList<>();
		members.add( sourceJerome );
		members.add( sourceEmmanuel );
		sourceJUG.setMembers( members );
		session.persist( sourceJUG );
		session.flush();

		assertThat( getNumberOfEntities( session ) ).isEqualTo( 3 );
		transaction.commit();
		assertThat( getNumberOfEntities( session ) ).isEqualTo( 3 );
		assertThat( getNumberOfAssociations( sessionFactory ) ).isEqualTo( 1 );

		session.clear();
		assertThat( JUGBinaryStore.store.size() ).isEqualTo( 1 );
		logger.debugf( "JUG keys: %s", JUGBinaryStore.store.keySet() );
		BinaryObject jugBinaryObject = JUGBinaryStore.store.get( sourceJUG.getId() );

		assertThat( jugBinaryObject.hasField( "members" ) ).isEqualTo( true );
		Collection<?> membersCollection =  jugBinaryObject.field( "members" );
		assertThat( membersCollection.size() ).isEqualTo( 2 );
		assertThat( membersCollection.contains( sourceEmmanuel.getId() ) ).isEqualTo( true );
		assertThat( membersCollection.contains( sourceJerome.getId() ) ).isEqualTo( true );

		assertThat( MemberBinaryStore.store.size() ).isEqualTo( 2 );
		logger.debugf( "Member keys: %s", MemberBinaryStore.store.keySet() );

		//need remove objects from memory
		IgniteDatastoreProvider igniteDatastoreProvider = IgniteTestHelper.getProvider( sessionFactory);
		igniteDatastoreProvider.clearCache( "CacheStoreJUG" );
		igniteDatastoreProvider.clearCache( "CacheStoreMember" );
		logger.info( "===================remove objects from memory=============================" );


		transaction = session.beginTransaction();
		//load link owners

		CacheStoreJUG jugFromCache = session.get( CacheStoreJUG.class, sourceJUG.getId() );
		assertNotNull(jugFromCache);
		assertThat( jugFromCache.getMembers().size() ).isEqualTo( 2 );
		assertThat( jugFromCache.getMembers().get( 0 ).getId() ).isEqualTo( sourceJerome.getId() );
		assertThat( jugFromCache.getMembers().get( 1 ).getId() ).isEqualTo( sourceEmmanuel.getId() );

		sourceEmmanuel = session.get( CacheStoreMember.class, sourceEmmanuel.getId() );
		sourceJUG = sourceEmmanuel.getMemberOf();
		session.delete( sourceEmmanuel );
		sourceJerome = session.get( CacheStoreMember.class, sourceJerome.getId() );
		session.delete( sourceJerome );
		session.delete( sourceJUG );
		transaction.commit();

		assertThat( getNumberOfEntities( session ) ).isEqualTo( 0 );
		assertThat( getNumberOfAssociations( sessionFactory ) ).isEqualTo( 0 );

		session.close();
		checkCleanCache();

	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				CacheStoreJUG.class, CacheStoreMember.class
		};
	}
}
