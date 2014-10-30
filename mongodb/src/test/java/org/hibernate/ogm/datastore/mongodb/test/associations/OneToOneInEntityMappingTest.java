/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.associations;

import static org.hibernate.ogm.datastore.mongodb.utils.MongoDBTestHelper.assertDbObject;

import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.backendtck.associations.onetoone.Husband;
import org.hibernate.ogm.backendtck.associations.onetoone.Wife;
import org.hibernate.ogm.cfg.DocumentStoreProperties;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.utils.OgmTestCase;
import org.junit.Test;

/**
 * Test for the persistent format of One-To-One associations.
 *
 * @author Gunnar Morling
 * @author Emmanuel Bernard
 */
public class OneToOneInEntityMappingTest extends OgmTestCase {

	@Test
	public void testBidirectionalManyToOneMapping() throws Exception {
		OgmSession session = openSession();
		Transaction transaction = session.beginTransaction();

		// Given, When
		Husband husband = new Husband( "alex" );
		husband.setName( "Alex" );
		session.persist( husband );

		Wife wife = new Wife( "bea" );
		wife.setName( "Bea" );
		husband.setWife( wife );
		wife.setHusband( husband );
		session.persist( wife );

		transaction.commit();
		session.clear();

		transaction = session.beginTransaction();

		// Then
		assertDbObject(
				session.getSessionFactory(),
				// collection
				"Wife",
				// query
				"{ '_id' : 'bea' }",
				// expected
				"{ " +
					"'_id' : 'bea', " +
					"'name' : 'Bea'," +
					"'husband' : 'alex'" +
				"}"
		);

		assertDbObject(
				session.getSessionFactory(),
				// collection
				"Husband",
				// query
				"{ '_id' : 'alex' }",
				// expected
				"{ " +
					"'_id' : 'alex', " +
					"'name' : 'Alex'," +
					"'wife' : 'bea'" +
				"}"
		);

		// Clean-Up
		husband = (Husband) session.get( Husband.class, husband.getId() );
		wife = (Wife) session.get( Wife.class, wife.getId() );
		session.delete( wife );
		session.delete( husband );

		transaction.commit();
		session.close();
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.getProperties().put(
				DocumentStoreProperties.ASSOCIATIONS_STORE,
				AssociationStorageType.IN_ENTITY
		);
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Husband.class, Wife.class };
	}
}
