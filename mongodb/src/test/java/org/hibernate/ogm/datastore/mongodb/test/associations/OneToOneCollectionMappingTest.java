/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.associations;

import static org.hibernate.ogm.datastore.mongodb.utils.MongoDBTestHelper.assertDocument;

import java.util.Map;

import org.hibernate.Transaction;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.backendtck.associations.onetoone.Husband;
import org.hibernate.ogm.backendtck.associations.onetoone.Wife;
import org.hibernate.ogm.datastore.document.cfg.DocumentStoreProperties;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.mongodb.MongoDBProperties;
import org.hibernate.ogm.datastore.mongodb.options.AssociationDocumentStorageType;
import org.hibernate.ogm.utils.OgmTestCase;
import org.junit.Test;

/**
 * Test for the persistent format of One-To-One associations.
 *
 * @author Gunnar Morling
 * @author Emmanuel Bernard
 */
public class OneToOneCollectionMappingTest extends OgmTestCase {

	@Test
	public void testAssociationStorageSettingIsIgnoredForBidirectionalManyToOneMapping() throws Exception {
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
		assertDocument(
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

		assertDocument(
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
	protected void configure(Map<String, Object> settings) {
		settings.put(
				DocumentStoreProperties.ASSOCIATIONS_STORE,
				AssociationStorageType.ASSOCIATION_DOCUMENT
		);
		settings.put(
				MongoDBProperties.ASSOCIATION_DOCUMENT_STORAGE,
				AssociationDocumentStorageType.COLLECTION_PER_ASSOCIATION
		);
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Husband.class, Wife.class };
	}
}
