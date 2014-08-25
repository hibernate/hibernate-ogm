/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test.mapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import org.hibernate.ogm.backendtck.id.Label;
import org.hibernate.ogm.backendtck.id.News;
import org.hibernate.ogm.backendtck.id.NewsID;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Davide D'Alto
 */
public class CompositeEmbeddedITest extends Neo4jJpaTestCase {

	final NewsID newsOgmID = new NewsID( "How to use Hibernate OGM ?", "Guillaume" );
	final List<Label> newsOgmLabels = labels( "OGM", "hibernate" );
	final News newsOGM = new News( newsOgmID, "Simple, just like ORM but with a NoSQL database", newsOgmLabels );

	@Before
	public void prepareDb() throws Exception {

		boolean operationSuccessful = false;
		getTransactionManager().begin();
		final EntityManager em = getFactory().createEntityManager();

		try {
			em.persist( newsOGM );
			operationSuccessful = true;
		}
		finally {
			commitOrRollback( operationSuccessful );
		}
		em.clear();
		em.close();
	}

	@Test
	public void testMapping() throws Exception {
		assertNumberOfNodes( 4 );
		assertRelationships( 2 );

		String sequenceNode = "(s:SEQUENCE { sequence_name: {s}.sequence_name, next_val: {s}.next_val })";
		String newsNode = "(n:News:ENTITY {`newsId.author`: {n}.`newsId.author`, `newsId.title`: {n}.`newsId.title`, content: {n}.content})";
		String labelNode = "(l:Label:ENTITY {id: {l}.id, name: {l}.name, news_author_fk: {l}.news_author_fk, news_topic_fk: {l}.news_topic_fk })";

		assertExpectedMapping( "s", sequenceNode, params( 0 ) );
		assertExpectedMapping( "n", newsNode, params( 0 ) );
		assertExpectedMapping( "l", labelNode, params( 0 ) );
		assertExpectedMapping( "l", labelNode, params( 1 ) );

		assertExpectedMapping( "r", newsNode + " - [r:labels] - " + labelNode, params( 0 ) );
		assertExpectedMapping( "r", newsNode + " - [r:labels] - " + labelNode, params( 1 ) );
	}

	private Map<String, Object> params(int labelIndex) {
		Map<String, Object> sequenceProperties = new HashMap<String, Object>();
		sequenceProperties.put( "sequence_name", "hibernate_sequence" );
		sequenceProperties.put( "next_val", 3 );

		Map<String, Object> labelProperties = labelProperties( newsOGM.getLabels().get( labelIndex ) );

		Map<String, Object> newsProperties = new HashMap<String, Object>();
		newsProperties.put( "newsId.author", newsOGM.getNewsId().getAuthor() );
		newsProperties.put( "newsId.title", newsOGM.getNewsId().getTitle() );
		newsProperties.put( "content", newsOGM.getContent() );

		Map<String, Object> params = new HashMap<String, Object>();
		params.put( "s", sequenceProperties );
		params.put( "l", labelProperties );
		params.put( "n", newsProperties );
		return params;
	}

	private Map<String, Object> labelProperties(Label label) {
		Map<String, Object> labelProperties = new HashMap<String, Object>();
		labelProperties.put( "id", label.getId() );
		labelProperties.put( "name", label.getName() );
		labelProperties.put( "news_author_fk", newsOgmID.getAuthor() );
		labelProperties.put( "news_topic_fk", newsOgmID.getTitle() );
		return labelProperties;
	}

	@Override
	public Class<?>[] getEntities() {
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
