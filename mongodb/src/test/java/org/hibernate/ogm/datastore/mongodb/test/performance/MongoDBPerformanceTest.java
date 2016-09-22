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
import org.hibernate.ogm.datastore.impl.DatastoreProviderType;
import org.hibernate.ogm.utils.BytemanHelper;
import org.hibernate.ogm.utils.BytemanHelperStateCleanup;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.SkipByDatastoreProvider;
import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMRules;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Guillaume Smet
 */
@SkipByDatastoreProvider({ DatastoreProviderType.FONGO })
public class MongoDBPerformanceTest extends OgmTestCase {

	@Rule
	public BytemanHelperStateCleanup bytemanState = new BytemanHelperStateCleanup();

	@Test
	@BMRules(rules = {
			@BMRule(
					targetClass = "com.mongodb.DBCollection",
					targetMethod = "insert(java.util.List, com.mongodb.WriteConcern)",
					helper = "org.hibernate.ogm.utils.BytemanHelper",
					action = "countInvocation(\"update\")",
					name = "countInsert"),
			@BMRule(targetClass = "com.mongodb.DBCollection",
					targetMethod = "update(com.mongodb.DBObject, com.mongodb.DBObject, boolean, boolean, com.mongodb.WriteConcern)",
					helper = "org.hibernate.ogm.utils.BytemanHelper",
					action = "countInvocation(\"update\")",
					name = "countUpdate"),
			@BMRule(targetClass = "com.mongodb.DBCollection",
					targetMethod = "update(com.mongodb.DBObject, com.mongodb.DBObject, boolean, boolean, com.mongodb.WriteConcern)",
					helper = "org.hibernate.ogm.utils.BytemanHelper",
					action = "countInvocation(\"update\")",
					name = "countUpdate"),
			@BMRule(targetClass = "com.mongodb.DBCollection",
					targetMethod = "findAndModify(com.mongodb.DBObject, com.mongodb.DBObject)",
					helper = "org.hibernate.ogm.utils.BytemanHelper",
					action = "countInvocation(\"update\")",
					name = "countFindAndModify"),
			@BMRule(targetClass = "com.mongodb.DBCollection",
					targetMethod = "findOne(com.mongodb.DBObject, com.mongodb.DBObject, com.mongodb.ReadPreference)",
					helper = "org.hibernate.ogm.utils.BytemanHelper",
					action = "countInvocation(\"load\")",
					name = "countFindOne")
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
		assertThat( updateInvocationCount ).isEqualTo( 3 );

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
