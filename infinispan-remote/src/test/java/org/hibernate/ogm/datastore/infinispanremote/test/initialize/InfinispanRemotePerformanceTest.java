/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.test.initialize;

import static org.fest.assertions.Assertions.assertThat;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.datastore.infinispanremote.utils.InfinispanRemoteServerRunner;
import org.hibernate.ogm.utils.BytemanHelper;
import org.hibernate.ogm.utils.BytemanHelperStateCleanup;
import org.hibernate.ogm.utils.OgmTestCase;
import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMRules;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(InfinispanRemoteServerRunner.class)
public class InfinispanRemotePerformanceTest extends OgmTestCase {

	@Rule
	public BytemanHelperStateCleanup bytemanState = new BytemanHelperStateCleanup();

	@Test
	@BMRules(rules = {
			@BMRule(targetClass = "org.infinispan.commons.api.BasicCache",
					isInterface = true,
					targetMethod = "put(Object, Object)",
					helper = "org.hibernate.ogm.utils.BytemanHelper",
					action = "countInvocation(\"put\")",
					name = "update"),
			@BMRule(targetClass = "org.infinispan.commons.api.BasicCache",
					isInterface = true,
					targetMethod = "putIfAbsent(Object, Object)",
					helper = "org.hibernate.ogm.utils.BytemanHelper",
					action = "countInvocation(\"putIfAbsent\")",
					name = "insert"),
			@BMRule(targetClass = "org.infinispan.client.hotrod.impl.RemoteCacheImpl",
					isInterface = false,
					targetMethod = "remove(Object)",
					helper = "org.hibernate.ogm.utils.BytemanHelper",
					action = "countInvocation(\"remove\")",
					name = "remove"),
			@BMRule(targetClass = "org.infinispan.client.hotrod.impl.RemoteCacheImpl",
					targetMethod = "getVersioned(Object)",
					isInterface = false,
					helper = "org.hibernate.ogm.utils.BytemanHelper",
					action = "countInvocation(\"getVersioned\")",
					name = "getVersioned")
	})
	public void testNumberOfCallsToDatastore() throws Exception {
		DisneyGrandMother grandMother = new DisneyGrandMother( "Grandma Duck" );

		// insert entity with embedded collection
		try ( Session session = openSession() ) {
			Transaction tx = session.beginTransaction();
			DisneyGrandChild donald = new DisneyGrandChild();
			donald.setName( "Donald Duck" );
			DisneyGrandChild gus = new DisneyGrandChild();
			gus.setName( "Gus Goose" );
			grandMother.getGrandChildren().add( gus );
			grandMother.getGrandChildren().add( donald );
			session.persist( grandMother );
			tx.commit();
		}

		// Load the entity
		int getEntityInvocationCount = BytemanHelper.getAndResetInvocationCount( "getVersioned" );
		assertThat( getEntityInvocationCount ).isEqualTo( 1 );

		// Insert the entity
		int storeEntityInvocationCount = BytemanHelper.getAndResetInvocationCount( "putIfAbsent" );
		assertThat( storeEntityInvocationCount ).isEqualTo( 1 );

		// Insert embeddeds
		int putInvocationCount = BytemanHelper.getAndResetInvocationCount( "put" );
		assertThat( putInvocationCount ).isEqualTo( 2 );

		// Nothing to remove so far
		int removeInvocationCount = BytemanHelper.getAndResetInvocationCount( "remove" );
		assertThat( removeInvocationCount ).isEqualTo( 0 );

		// Remove one of the elements
		try ( Session session = openSession() ) {
			Transaction tx = session.beginTransaction();
			grandMother = (DisneyGrandMother) session.get( DisneyGrandMother.class, grandMother.getId() );
			grandMother.getGrandChildren().remove( 0 );
			tx.commit();
			session.clear();
		}

		// Load the entity
		getEntityInvocationCount = BytemanHelper.getAndResetInvocationCount( "getVersioned" );
		assertThat( getEntityInvocationCount ).isEqualTo( 1 );

		// We are not adding anything new
		storeEntityInvocationCount = BytemanHelper.getAndResetInvocationCount( "putIfAbsent" );
		assertThat( storeEntityInvocationCount ).isEqualTo( 0 );

		// Update the index of remaining element
		putInvocationCount = BytemanHelper.getAndResetInvocationCount( "put" );
		assertThat( putInvocationCount ).isEqualTo( 1 );

		// Remove the element
		removeInvocationCount = BytemanHelper.getAndResetInvocationCount( "remove" );
		assertThat( removeInvocationCount ).isEqualTo( 1 );

		// Assert removal has been propagated
		try ( Session session = openSession() ) {
			Transaction tx = session.beginTransaction();
			grandMother = (DisneyGrandMother) session.get( DisneyGrandMother.class, grandMother.getId() );
			assertThat( grandMother.getGrandChildren() ).onProperty( "name" ).containsExactly( "Donald Duck" );

			session.delete( grandMother );
			tx.commit();
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{
				DisneyGrandMother.class,
		};
	}
}
