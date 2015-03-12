/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test.mapping;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;

import org.hibernate.ogm.backendtck.queries.AnEmbeddable;
import org.hibernate.ogm.backendtck.queries.AnotherEmbeddable;
import org.hibernate.ogm.backendtck.queries.EmbeddedCollectionItem;
import org.hibernate.ogm.backendtck.queries.WithEmbedded;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the mapping of embeddable collections in Neo4j
 *
 * @author Davide D'Alto
 */
public class ElementCollectionMappingTest extends Neo4jJpaTestCase {

	@Before
	public void prepareDB() throws Exception {
		final EntityManager em = getFactory().createEntityManager();
		em.getTransaction().begin();

		WithEmbedded with = new WithEmbedded( 1L, null );
		with.setAnEmbeddable( new AnEmbeddable( "embedded 1", new AnotherEmbeddable( "string 1", 1 ) ) );
		with.setYetAnotherEmbeddable( new AnEmbeddable( "embedded 2" ) );
		with.setAnEmbeddedCollection( Arrays.asList( new EmbeddedCollectionItem( "item[0]", "secondItem[0]", null ), new EmbeddedCollectionItem( "item[1]", null, new AnotherEmbeddable( "string[1][0]", 10 ) ) ) );
		with.setAnotherEmbeddedCollection( Arrays.asList( new EmbeddedCollectionItem( "another[0]", null, null ), new EmbeddedCollectionItem( "another[1]", null, null ) ) );

		em.persist( with );
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testEmbeddedCollectionNodesMapping() throws Exception {
		assertNumberOfNodes( 8 );
		assertRelationships( 7 );

		String withEmbeddedNode = "(w:WithEmbedded:ENTITY { id: {w}.id })";
		String anEmbeddableNode = "(e:EMBEDDED {embeddedString: {e}.embeddedString})";
		String anotherEmbeddableNode = "(ae:EMBEDDED {embeddedString: {ae}.embeddedString, embeddedInteger: {ae}.embeddedInteger})";
		String yetAnotherEmbeddableNode = "(yae:EMBEDDED {embeddedString: {yae}.embeddedString})";

		String anEmbeddedCollectionNode1 = "(ec1:EMBEDDED {embeddedString: {ec1}.embeddedString, embeddedInteger: {ec1}.embeddedInteger, item: {ec1}.item})";
		String anEmbeddedCollectionNode2 = "(ec2:EMBEDDED {item: {ec2}.item, anotherItem: {ec2}.anotherItem})";

		String anotherEmbeddedCollectionNode1 = "(aec1:EMBEDDED {item: {aec1}.item})";
		String anotherEmbeddedCollectionNode2 = "(aec2:EMBEDDED {item: {aec2}.item})";

		Map<String, Object> withEmbeddedNodeProperties = new HashMap<String, Object>();
		withEmbeddedNodeProperties.put( "id", 1L );

		Map<String, Object> anEmbeddableNodeProperties = new HashMap<String, Object>();
		anEmbeddableNodeProperties.put( "embeddedString", "embedded 1" );

		Map<String, Object> anotherEmbeddableNodeProperties = new HashMap<String, Object>();
		anotherEmbeddableNodeProperties.put( "embeddedString", "string 1" );
		anotherEmbeddableNodeProperties.put( "embeddedInteger", 1 );

		Map<String, Object> yetAnotherEmbeddableNodeProperties = new HashMap<String, Object>();
		yetAnotherEmbeddableNodeProperties.put( "embeddedString", "embedded 2" );

		Map<String, Object> anEmbeddedCollectionNodeProperties1 = new HashMap<String, Object>();
		anEmbeddedCollectionNodeProperties1.put( "embeddedString", "string[1][0]" );
		anEmbeddedCollectionNodeProperties1.put( "embeddedInteger", 10 );
		anEmbeddedCollectionNodeProperties1.put( "item", "item[1]" );

		Map<String, Object> anEmbeddedCollectionNodeProperties2 = new HashMap<String, Object>();
		anEmbeddedCollectionNodeProperties2.put( "item", "item[0]" );
		anEmbeddedCollectionNodeProperties2.put( "anotherItem", "secondItem[0]" );

		Map<String, Object> anotherEmbeddedCollectionNodeProperties1 = new HashMap<String, Object>();
		anotherEmbeddedCollectionNodeProperties1.put( "item", "another[0]" );

		Map<String, Object> anotherEmbeddedCollectionNodeProperties2 = new HashMap<String, Object>();
		anotherEmbeddedCollectionNodeProperties2.put( "item", "another[1]" );

		Map<String, Object> params = new HashMap<String, Object>();
		params.put( "w", withEmbeddedNodeProperties );
		params.put( "e", anEmbeddableNodeProperties );
		params.put( "ae", anotherEmbeddableNodeProperties );
		params.put( "yae", yetAnotherEmbeddableNodeProperties );

		params.put( "ec1", anEmbeddedCollectionNodeProperties1 );
		params.put( "ec2", anEmbeddedCollectionNodeProperties2 );

		params.put( "aec1", anotherEmbeddedCollectionNodeProperties1 );
		params.put( "aec2", anotherEmbeddedCollectionNodeProperties2 );

		assertExpectedMapping( "w", withEmbeddedNode, params );
		assertExpectedMapping( "e", anEmbeddableNode, params );
		assertExpectedMapping( "ae", anotherEmbeddableNode, params );
		assertExpectedMapping( "yae", yetAnotherEmbeddableNode, params );

		assertExpectedMapping( "ec1", anEmbeddedCollectionNode1, params );
		assertExpectedMapping( "ec2", anEmbeddedCollectionNode2, params );

		assertExpectedMapping( "aec1", anotherEmbeddedCollectionNode1, params );
		assertExpectedMapping( "aec2", anotherEmbeddedCollectionNode2, params );

		assertExpectedMapping( "r", withEmbeddedNode + " - [r:anEmbeddable] -> " + anEmbeddableNode + " - [:anotherEmbeddable] -> " + anotherEmbeddableNode, params );
		assertExpectedMapping( "r", withEmbeddedNode + " - [r:yetAnotherEmbeddable] -> " + yetAnotherEmbeddableNode, params );

		assertExpectedMapping( "r", withEmbeddedNode + " - [r:anEmbeddedCollection] -> " + anEmbeddedCollectionNode1, params );
		assertExpectedMapping( "r", withEmbeddedNode + " - [r:anEmbeddedCollection] -> " + anEmbeddedCollectionNode2, params );

		assertExpectedMapping( "r", withEmbeddedNode + " - [r:anotherEmbeddedCollection] -> " + anotherEmbeddedCollectionNode1, params );
		assertExpectedMapping( "r", withEmbeddedNode + " - [r:anotherEmbeddedCollection] -> " + anotherEmbeddedCollectionNode2, params );
	}

	@Override
	public Class<?>[] getEntities() {
		return new Class[] { WithEmbedded.class };
	}
}
