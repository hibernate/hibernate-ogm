/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.test.performance;

import static org.fest.assertions.Assertions.assertThat;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.backendtck.associations.collection.types.Child;
import org.hibernate.ogm.backendtck.associations.collection.types.GrandChild;
import org.hibernate.ogm.backendtck.associations.collection.types.GrandMother;
import org.hibernate.ogm.datastore.redis.test.RedisOgmTestCase;
import org.hibernate.ogm.utils.BytemanHelper;
import org.hibernate.ogm.utils.BytemanHelperStateCleanup;
import org.hibernate.ogm.utils.GridDialectType;
import org.hibernate.ogm.utils.SkipByGridDialect;
import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMRules;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Guillaume Smet
 */
@SkipByGridDialect({ GridDialectType.REDIS_HASH })
public class RedisPerformanceTest extends RedisOgmTestCase {

	@Rule
	public BytemanHelperStateCleanup bytemanState = new BytemanHelperStateCleanup();

	@Test
	@BMRules(rules = {
			@BMRule(targetClass = "org.hibernate.ogm.datastore.redis.impl.json.JsonEntityStorageStrategy",
					targetMethod = "storeEntity(java.lang.String, org.hibernate.ogm.datastore.redis.dialect.value.Entity)",
					helper = "org.hibernate.ogm.utils.BytemanHelper",
					action = "countInvocation(\"storeEntity\")",
					name = "countStoreEntity"),
			@BMRule(targetClass = "org.hibernate.ogm.datastore.redis.impl.json.JsonEntityStorageStrategy",
					targetMethod = "getEntity(java.lang.String)",
					helper = "org.hibernate.ogm.utils.BytemanHelper",
					action = "countInvocation(\"getEntity\")",
					name = "countGetEntity"),
			@BMRule(targetClass = "org.hibernate.ogm.datastore.redis.RedisJsonDialect",
					targetMethod = "getCurrentTtl(java.lang.String)",
					helper = "org.hibernate.ogm.utils.BytemanHelper",
					action = "countInvocation(\"getTtl\")",
					name = "countGetTtl")
	})
	public void testNumberOfCallsToDatastore() throws Exception {
		//insert entity with embedded collection
		Session session = openSession();
		Transaction tx = session.beginTransaction();
		GrandChild luke = new GrandChild();
		luke.setName( "Luke" );
		GrandChild leia = new GrandChild();
		leia.setName( "Leia" );
		GrandMother grandMother = new GrandMother();
		grandMother.getGrandChildren().add( luke );
		grandMother.getGrandChildren().add( leia );
		session.persist( grandMother );
		tx.commit();

		session.clear();

		int getEntityInvocationCount = BytemanHelper.getAndResetInvocationCount( "getEntity" );
		int storeEntityInvocationCount = BytemanHelper.getAndResetInvocationCount( "storeEntity" );
		int pttlInvocationCount = BytemanHelper.getAndResetInvocationCount( "getTtl" );
		assertThat( getEntityInvocationCount ).isEqualTo( 3 );
		assertThat( storeEntityInvocationCount ).isEqualTo( 2 );
		assertThat( pttlInvocationCount ).isEqualTo( 2 );

		// Check that all the counters have been resetted to 0
		getEntityInvocationCount = BytemanHelper.getAndResetInvocationCount( "getEntity" );
		storeEntityInvocationCount = BytemanHelper.getAndResetInvocationCount( "storeEntity" );
		pttlInvocationCount = BytemanHelper.getAndResetInvocationCount( "getTtl" );
		assertThat( getEntityInvocationCount ).isEqualTo( 0 );
		assertThat( storeEntityInvocationCount ).isEqualTo( 0 );
		assertThat( pttlInvocationCount ).isEqualTo( 0 );

		//remove one of the elements and add a new one
		tx = session.beginTransaction();
		grandMother = (GrandMother) session.get( GrandMother.class, grandMother.getId() );
		grandMother.getGrandChildren().remove( 0 );
		tx.commit();
		session.clear();

		getEntityInvocationCount = BytemanHelper.getAndResetInvocationCount( "getEntity" );
		storeEntityInvocationCount = BytemanHelper.getAndResetInvocationCount( "storeEntity" );
		pttlInvocationCount = BytemanHelper.getAndResetInvocationCount( "getTtl" );
		assertThat( getEntityInvocationCount ).isEqualTo( 5 );
		assertThat( storeEntityInvocationCount ).isEqualTo( 3 );
		assertThat( pttlInvocationCount ).isEqualTo( 3 );

		//assert removal has been propagated
		tx = session.beginTransaction();
		grandMother = (GrandMother) session.get( GrandMother.class, grandMother.getId() );
		assertThat( grandMother.getGrandChildren() ).onProperty( "name" ).containsExactly( "Leia" );

		session.delete( grandMother );
		tx.commit();

		session.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				GrandMother.class,
				Child.class
		};
	}
}
