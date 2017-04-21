/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.mapping.associations;

import static org.hibernate.ogm.datastore.mongodb.utils.MongoDBTestHelper.assertDocument;

import java.io.Serializable;

import org.hibernate.Transaction;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.utils.OgmTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Davide D'Alto
 */
public class MultipleInverseAssociationOnTheSameEntityTest extends OgmTestCase {

	@Before
	public void setUpTestData() {
		try ( OgmSession session = openSession() ) {
			Transaction transaction = session.beginTransaction();

			Node root = new Node( "root" );
			Node child1 = new Node( "child1" );
			Node child2 = new Node( "child2" );

			NodeLink link1 = new NodeLink( "nl1" );
			link1.setSource( root );
			link1.setTarget( child1 );

			NodeLink link2 = new NodeLink( "nl2" );
			link2.setSource( root );
			link2.setTarget( child2 );

			root.getChildren().add( link1 );
			root.getChildren().add( link2 );

			session.persist( root );
			session.persist( child1 );
			session.persist( child2 );
			session.persist( link1 );
			session.persist( link2 );

			transaction.commit();
			session.clear();
		}
	}

	@After
	public void removeTestData() {
		delete( NodeLink.class, "nl1", "nl2" );
		delete( Node.class, "root", "child1", "child2" );
	}

	private void delete(Class<?> entityType, Serializable... ids) {
		try ( OgmSession session = openSession() ) {
			Transaction transaction = session.beginTransaction();
			for ( Serializable id : ids ) {
				session.delete( session.load( entityType, id ) );
			}
			transaction.commit();
		}
	}

	@Test
	public void booleanMapping() {
		try ( OgmSession session = openSession() ) {
			Transaction transaction = session.beginTransaction();

			assertDocument(
					session.getSessionFactory(),
					// collection
					"Node",
					// query
					"{ '_id' : 'root' }",
					// fields
					null,
					// expected
					"{ " +
						"'_id' : 'root', " +
						"'children' : [ 'nl1', 'nl2' ]" +
					"}"
			);

			assertDocument(
					session.getSessionFactory(),
					// collection
					"Node",
					// query
					"{ '_id' : 'child1' }",
					// fields
					null,
					// expected
					"{ " +
						"'_id' : 'child1', " +
					"}"
			);

			assertDocument(
					session.getSessionFactory(),
					// collection
					"Node",
					// query
					"{ '_id' : 'child2' }",
					// fields
					null,
					// expected
					"{ " +
						"'_id' : 'child2', " +
					"}"
			);

			assertDocument(
					session.getSessionFactory(),
					// collection
					"NodeLink",
					// query
					"{ '_id' : 'nl1' }",
					// fields
					null,
					// expected
					"{ " +
						"'_id' : 'nl1', " +
						"'source_name' : 'root', " +
						"'target_name' : 'child1', " +
					"}"
			);

			assertDocument(
					session.getSessionFactory(),
					// collection
					"NodeLink",
					// query
					"{ '_id' : 'nl2' }",
					// fields
					null,
					// expected
					"{ " +
						"'_id' : 'nl2', " +
						"'source_name' : 'root', " +
						"'target_name' : 'child2', " +
					"}"
			);

			transaction.commit();
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ Node.class, NodeLink.class };
	}
}
