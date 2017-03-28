/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.mapping;

import static org.hibernate.ogm.datastore.mongodb.utils.MongoDBTestHelper.assertDocument;

import org.hibernate.Transaction;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.backendtck.associations.collection.types.Child;
import org.hibernate.ogm.backendtck.associations.collection.types.GrandChild;
import org.hibernate.ogm.backendtck.associations.collection.types.GrandMother;
import org.hibernate.ogm.utils.OgmTestCase;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @author Gunnar Morling
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

		assertDocument(
				session.getSessionFactory(),
				// collection
				"GrandMother",
				// query
				"{ '_id' : '" + granny.getId() + "' }",
				// fields
				null,
				// expected
				"{ " +
					"'_id' : '" + granny.getId() + "', " +
					"'grandChildren' : [" +
						"{ 'name' : 'Luke', 'birthorder' : 0 }," +
						"{ 'name' : 'Leia', 'birthorder' : 1 }" +
					"]" +
				"}"
		);

		transaction.commit();
		session.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { GrandMother.class, Child.class };
	}
}
