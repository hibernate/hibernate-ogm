/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test.mapping;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import org.hibernate.ogm.backendtck.id.Label;
import org.hibernate.ogm.backendtck.id.News;
import org.hibernate.ogm.backendtck.id.NewsID;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.NodeLabel;
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

		String newsNode = "(:News:ENTITY {`newsId.author`: '" + newsOGM.getNewsId().getAuthor() + "', `newsId.title`: '" + newsOGM.getNewsId().getTitle() + "', content: '" + newsOGM.getContent() + "'})";
		String labelNode0 = "(:Label:ENTITY {id: " + newsOgmLabels.get( 0 ).getId() + ", name: '" + newsOgmLabels.get( 0 ).getName() + "' })";
		String labelNode1 = "(:Label:ENTITY {id: " + newsOgmLabels.get( 1 ).getId() + ", name: '" + newsOgmLabels.get( 1 ).getName() + "' })";
		String sequenceNode = "(:" + NodeLabel.SEQUENCE + " { sequence_name: 'hibernate_sequence' })";

		assertExpectedMapping( sequenceNode );
		assertExpectedMapping( newsNode + " - [:Label] - " + labelNode0 );
		assertExpectedMapping( newsNode + " - [:Label] - " + labelNode1 );
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
