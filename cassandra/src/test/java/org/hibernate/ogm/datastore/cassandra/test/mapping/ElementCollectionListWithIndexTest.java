/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.cassandra.test.mapping;

import static org.hibernate.ogm.datastore.cassandra.utils.CassandraTestHelper.rowAssertion;

import org.hibernate.Transaction;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.backendtck.associations.collection.types.GrandChild;
import org.hibernate.ogm.backendtck.associations.collection.types.GrandMother;
import org.hibernate.ogm.utils.OgmTestCase;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Nicola Ferraro
 */
public class ElementCollectionListWithIndexTest extends OgmTestCase {

	private GrandMother granny;

	@Before
	public void prepareDb() throws Exception {
		OgmSession session = openSession();
		Transaction transaction = session.beginTransaction();

		GrandChild luke = new GrandChild();
		luke.setName( "Luke" );

		GrandChild leia = new GrandChild();
		leia.setName( "Leia" );

		granny = new GrandMother();
		granny.getGrandChildren().add( luke );
		granny.getGrandChildren().add( leia );

		session.persist( granny );
		transaction.commit();
		session.close();
	}

	@Test
	public void testMapping() throws Exception {
		OgmSession session = openSession();
		Transaction transaction = session.beginTransaction();

		rowAssertion( session.getSessionFactory(), "GrandMother" )
				.keyColumn( "id", granny.getId() )
				.assertNoOtherColumnPresent()
				.execute();

		rowAssertion( session.getSessionFactory(), "GrandMother_grandChildren" )
				.keyColumn( "GrandMother_id", granny.getId() )
				.keyColumn( "birthorder", 0 )
				.assertColumn( "name", "Luke" )
				.assertNoOtherColumnPresent()
				.execute();

		rowAssertion( session.getSessionFactory(), "GrandMother_grandChildren" )
				.keyColumn( "GrandMother_id", granny.getId() )
				.keyColumn( "birthorder", 1 )
				.assertColumn( "name", "Leia" )
				.assertNoOtherColumnPresent()
				.execute();

		transaction.commit();
		session.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ GrandMother.class };
	}
}
