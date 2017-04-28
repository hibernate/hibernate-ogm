/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.performance;

import static org.fest.assertions.Assertions.assertThat;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.backendtck.associations.collection.types.Child;
import org.hibernate.ogm.backendtck.associations.collection.types.GrandChild;
import org.hibernate.ogm.backendtck.associations.collection.types.GrandMother;
import org.hibernate.ogm.utils.BytemanHelper;
import org.hibernate.ogm.utils.BytemanHelperStateCleanup;
import org.hibernate.ogm.utils.OgmTestCase;
import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMRules;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Guillaume Smet
 */
public class MongoDBPerformanceTest extends OgmTestCase {

	private static final String MONGO_COLLECTION = "com.mongodb.client.MongoCollection";
	private static final String HELPER = "org.hibernate.ogm.utils.BytemanHelper";
	private static final String BSON_DOCUMENT = "org.bson.conversions.Bson";

	@Rule
	public BytemanHelperStateCleanup bytemanState = new BytemanHelperStateCleanup();

	@Test
	@BMRules(rules = {
			@BMRule(
					targetClass = MONGO_COLLECTION,
					isInterface = true,
					targetMethod = "insertMany(java.util.List)",
					helper = HELPER,
					action = "countInvocation(\"update\")",
					name = "countInsertMany"),
			@BMRule(targetClass = MONGO_COLLECTION,
					isInterface = true,
					targetMethod = "updateOne(" + BSON_DOCUMENT + "," + BSON_DOCUMENT + ",com.mongodb.client.model.UpdateOptions)",
					helper = HELPER,
					action = "countInvocation(\"update\")",
					name = "countUpdateOne"),
			@BMRule(targetClass = MONGO_COLLECTION,
					isInterface = true,
					targetMethod = "find(" + BSON_DOCUMENT + ")",
					helper = HELPER,
					action = "countInvocation(\"load\")",
					name = "countFind")
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

		int loadInvocationCount = BytemanHelper.getAndResetInvocationCount( "load" );
		int updateInvocationCount = BytemanHelper.getAndResetInvocationCount( "update" );
		assertThat( loadInvocationCount ).isEqualTo( 0 );
		assertThat( updateInvocationCount ).isEqualTo( 1 );

		// Check that all the counters have been reset to 0
		loadInvocationCount = BytemanHelper.getAndResetInvocationCount( "load" );
		updateInvocationCount = BytemanHelper.getAndResetInvocationCount( "update" );
		assertThat( loadInvocationCount ).isEqualTo( 0 );
		assertThat( updateInvocationCount ).isEqualTo( 0 );

		//remove one of the elements and add a new one
		tx = session.beginTransaction();
		grandMother = (GrandMother) session.get( GrandMother.class, grandMother.getId() );
		grandMother.getGrandChildren().remove( 0 );
		tx.commit();
		session.clear();

		loadInvocationCount = BytemanHelper.getAndResetInvocationCount( "load" );
		updateInvocationCount = BytemanHelper.getAndResetInvocationCount( "update" );
		assertThat( loadInvocationCount ).isEqualTo( 1 );
		assertThat( updateInvocationCount ).isEqualTo( 1 );

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
