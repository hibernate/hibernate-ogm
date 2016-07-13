/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test.mapping;

import static org.hibernate.ogm.datastore.neo4j.dialect.impl.NodeLabel.ENTITY;
import static org.hibernate.ogm.datastore.neo4j.dialect.impl.NodeLabel.SEQUENCE;
import static org.hibernate.ogm.datastore.neo4j.test.dsl.GraphAssertions.node;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import org.hibernate.ogm.backendtck.id.Label;
import org.hibernate.ogm.backendtck.id.News;
import org.hibernate.ogm.backendtck.id.NewsID;
import org.hibernate.ogm.datastore.neo4j.test.dsl.NodeForGraphAssertions;
import org.hibernate.ogm.datastore.neo4j.test.dsl.RelationshipsChainForGraphAssertions;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Davide D'Alto
 */
public class CompositeEmbeddedIdTest extends Neo4jJpaTestCase {

	final NewsID newsOgmID = new NewsID( "How to use Hibernate OGM ?", "Guillaume" );
	final List<Label> newsOgmLabels = labels( "OGM", "hibernate" );
	final News newsOGM = new News( newsOgmID, "Simple, just like ORM but with a NoSQL database", newsOgmLabels );

	@Before
	public void prepareDb() throws Exception {
		EntityManager em = getFactory().createEntityManager();
		em.getTransaction().begin();
		em.persist( newsOGM );
		em.getTransaction().commit();
		em.clear();
		em.close();
	}

	@Test
	public void testMapping() throws Exception {
		NodeForGraphAssertions sequenceNode = node( "sequence", SEQUENCE.name() )
				.property( "sequence_name", "hibernate_sequence" )
				.property( "next_val", 3 );

		NodeForGraphAssertions newsNode = node( "news", News.class.getSimpleName(), ENTITY.name() )
				.property( "newsId.author", newsOgmID.getAuthor() )
				.property( "newsId.title", newsOgmID.getTitle() )
				.property( "content", newsOGM.getContent() );

		NodeForGraphAssertions label1Node = node( "label1", Label.class.getSimpleName(), ENTITY.name() )
				.property( "id", newsOgmLabels.get( 0 ).getId() )
				.property( "name", newsOgmLabels.get( 0 ).getName() )
				.property( "news_author_fk", newsOgmID.getAuthor() )
				.property( "news_topic_fk", newsOgmID.getTitle() )
				;

		NodeForGraphAssertions label2Node = node( "label2", Label.class.getSimpleName(), ENTITY.name() )
				.property( "id", newsOgmLabels.get( 1 ).getId() )
				.property( "name", newsOgmLabels.get( 1 ).getName() )
				.property( "news_author_fk", newsOgmID.getAuthor() )
				.property( "news_topic_fk", newsOgmID.getTitle() )
				;

		RelationshipsChainForGraphAssertions relationship1 = newsNode.relationshipTo( label1Node, "labels" );
		RelationshipsChainForGraphAssertions relationship2 = newsNode.relationshipTo( label2Node, "labels" );

		assertThatOnlyTheseNodesExist( newsNode, label1Node, label2Node, sequenceNode );
		assertThatOnlyTheseRelationshipsExist( relationship1, relationship2 );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { News.class, Label.class };
	}

	protected List<Label> labels(String... names) {
		final List<Label> labels = new ArrayList<Label>();
		for ( String name : names ) {
			labels.add( new Label( name ) );
		}
		return labels;
	}

}
