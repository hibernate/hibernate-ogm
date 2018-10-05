/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.inheritance.tableperclass;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

import java.util.List;
import javax.persistence.PersistenceException;

import org.hibernate.ogm.datastore.mongodb.test.inheritance.singletable.SingleTableInheritanceTest;
import org.hibernate.ogm.utils.TestForIssue;
import org.hibernate.ogm.utils.jpa.OgmJpaTestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Tests table per class inheritance mapping
 *
 * @author Fabio Massimo Ercoli
 * @see SingleTableInheritanceTest
 */
@TestForIssue(jiraKey = "OGM-1425")
public class TablePerClassInheritanceTest extends OgmJpaTestCase {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private SimpleNode parent;
	private TextNode textChild1;
	private TextNode textChild2;
	private NodeLink linkToText1;
	private NodeLink linkToText2;

	@Before
	public void setUp() {
		parent = new SimpleNode();
		parent.setName( "root" );

		textChild1 = new TextNode();
		textChild1.setName( "child 1" );
		textChild1.setText( "a text for the first child" );

		textChild2 = new TextNode();
		textChild2.setName( "child 2" );
		textChild2.setText( "a text for the second child" );

		linkToText1 = new NodeLink();
		linkToText1.assignSource( parent );
		linkToText1.assignTarget( textChild1 );

		linkToText2 = new NodeLink();
		linkToText2.assignSource( parent );
		linkToText2.assignTarget( textChild2 );

		inTransaction( em -> {
			em.persist( parent );
			em.persist( textChild1 );
			em.persist( textChild2 );
			em.persist( linkToText1 );
			em.persist( linkToText2 );
		} );
	}

	@Test
	public void testSubclassReturnedByNativeQuery() {
		inTransaction( em -> {
			List<TextNode> textNodes = em.createNativeQuery( "db.TextNode.find( { $query : {}, $orderby : { name : 1 } } )", TextNode.class ).getResultList();
			assertThat( textNodes ).containsExactly( textChild1, textChild2 );
			assertThat( textNodes.get( 0 ).getText() ).isEqualTo( textChild1.getText() );
			assertThat( textNodes.get( 1 ).getText() ).isEqualTo( textChild2.getText() );
		} );
	}

	@Test
	public void testPolymorphismWithNativeQuery() {
		inTransaction( em -> {
			List<SimpleNode> simpleNodes = em.createNativeQuery( "db.SimpleNode.find( {} )", SimpleNode.class ).getResultList();
			assertThat( simpleNodes ).containsExactly( parent );
			assertThat( simpleNodes.get( 0 ).getChildren() ).containsOnly( linkToText1, linkToText2 );
		} );
	}

	@Test
	public void testPolymorphismWithNativeQueryWithoutDiscriminator() {
		thrown.expect( PersistenceException.class );
		thrown.expectMessage(
				"OGM000089: MongoDB does not support queries on polymorphic entities using TABLE_PER_CLASS inheritance strategy. You should try using SINGLE_TABLE instead." );

		inTransaction( em -> {
			em.createQuery( "from Node order by name" ).getResultList();
			fail( "Exception expected here" );
		} );
	}

	@After
	public void tearDown() {
		removeAll( parent, textChild1, textChild2, linkToText1, linkToText2 );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Node.class, SimpleNode.class, TextNode.class, NodeLink.class };
	}
}
