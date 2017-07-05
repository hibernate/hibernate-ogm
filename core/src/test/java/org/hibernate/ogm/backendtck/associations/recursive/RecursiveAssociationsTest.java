/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.associations.recursive;

import static org.fest.assertions.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.Transaction;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.TestForIssue;
import org.junit.Before;
import org.junit.Test;

/**
 * Creates a tree where each node has a certain number of children and then executes some CRUD operation on it.
 *
 * @author Davide D'Alto
 */
@TestForIssue(jiraKey = { "OGM-1284", "OGM-1292" })
public class RecursiveAssociationsTest extends OgmTestCase {

	int DEPTH = 3;
	int NUM_CHILDREN = 3;

	@Before
	public void before() {
		createTree();
	}

	/*
	 * Simple tree generation.
	 * The tree will look something like (depth 2, children 2):
	 *
	 *                          -- (NODE: 6)
	 *             -- (NODE: 4) |
	 *            |             -- (NODE: 5)
	 * (ROOT: 0) --
	 *            |             -- (NODE: 3)
	 *             -- (NODE: 1) |
	 *                          -- (LEAF: 2)
	 */
	private void createTree() {
		try ( OgmSession session = openSession() ) {
			AtomicInteger index = new AtomicInteger( 0 );
			Transaction tx = session.beginTransaction();
			TreeNode root = new TreeNode( name( index.get() ) );
			session.persist( root );
			createChildren( session, root, 1, index );
			tx.commit();
		}
	}

	private void createChildren(OgmSession session, TreeNode parent, int currentDepth, AtomicInteger index) {
		for ( int i = 0; i < NUM_CHILDREN; i++ ) {
			index.incrementAndGet();
			TreeNode child = createNode( parent, index.get() );
			session.persist( child );
			if ( currentDepth < DEPTH ) {
				createChildren( session, child, currentDepth + 1, index );
			}
		}
	}

	private String name(int index) {
		return "NODE: " + index;
	}

	private TreeNode createNode(TreeNode parent, int middle) {
		TreeNode child = new TreeNode( name( middle ) );
		parent.getChildren().add( child );
		child.setParent( parent );
		return child;
	}

	@Test
	public void test() throws Exception {
		try ( OgmSession session = openSession() ) {
			Transaction tx = session.beginTransaction();
			TreeNode root = session.load( TreeNode.class, name( 0 ) );
			assertThat( root.getChildren() ).hasSize( NUM_CHILDREN );

			TreeNode node = session.load( TreeNode.class, name( 1 ) );
			assertThat( node.getChildren() ).hasSize( NUM_CHILDREN );

			TreeNode leaf = session.load( TreeNode.class, name( 3 ) );
			assertThat( leaf.getChildren() ).isEmpty();

			tx.commit();
		}

		deleteNode( 2 );

		try ( OgmSession session = openSession() ) {
			Transaction tx = session.beginTransaction();
			TreeNode root = session.load( TreeNode.class, name( 0 ) );

			assertThat( root ).isNotNull();
			assertThat( root.getChildren() ).hasSize( NUM_CHILDREN );

			TreeNode node = session.load( TreeNode.class, name( 1 ) );
			assertThat( node.getChildren() ).hasSize( NUM_CHILDREN - 1 );

			tx.commit();
		}
	}

	private void deleteNode(int nodeId) {
		try ( OgmSession session = openSession() ) {
			Transaction tx = session.beginTransaction();
			session.delete( session.load( TreeNode.class, name( nodeId ) ) );
			tx.commit();
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{ TreeNode.class };
	}
}
