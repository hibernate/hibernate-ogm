/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.massindex;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.utils.GridDialectType.IGNITE;
import static org.hibernate.ogm.utils.GridDialectType.MONGODB;
import static org.hibernate.ogm.utils.GridDialectType.NEO4J_EMBEDDED;
import static org.hibernate.ogm.utils.GridDialectType.NEO4J_REMOTE;

import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityManager;

import org.apache.lucene.search.Query;
import org.hibernate.ogm.backendtck.id.NewsID;
import org.hibernate.ogm.backendtck.massindex.model.IndexedLabel;
import org.hibernate.ogm.backendtck.massindex.model.IndexedNews;
import org.hibernate.ogm.utils.SkipByGridDialect;
import org.hibernate.ogm.utils.jpa.GetterPersistenceUnitInfo;
import org.hibernate.ogm.utils.jpa.OgmJpaTestCase;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.junit.Test;

/**
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
@SkipByGridDialect( IGNITE )
public class AssociationMassIndexerTest extends OgmJpaTestCase {

	@Test
	@SkipByGridDialect(value = { MONGODB, NEO4J_EMBEDDED, NEO4J_REMOTE }, comment = "Uses embedded key which is currently not supported by the db query parsers")
	public void testEntityWithAssociationMassIndexing() throws Exception {
		populateDatastore();
		purgeAll( IndexedNews.class, IndexedLabel.class );
		startAndWaitMassIndexing( IndexedNews.class, IndexedLabel.class );

		assertEntityHasBeenIndexed();
		assertAssociatedElementsHaveBeenIndexed();
	}

	private void populateDatastore() throws Exception {
		List<IndexedLabel> labes = Arrays.asList( new IndexedLabel( "massindex" ), new IndexedLabel( "test" ) );
		IndexedNews news = new IndexedNews( new NewsID( "title", "author" ), "content" );
		news.setLabels( labes );
		EntityManager em = createEntityManager();
		em.getTransaction().begin();
		em.persist( news );
		em.getTransaction().commit();
		em.close();
	}

	private void assertEntityHasBeenIndexed() throws Exception {
		FullTextEntityManager fullTextEm = Search.getFullTextEntityManager( createEntityManager() );
		fullTextEm.getTransaction().begin();
		QueryBuilder queryBuilder = fullTextEm.getSearchFactory()
				.buildQueryBuilder()
				.forEntity( IndexedNews.class )
				.get();
		Query luceneQuery = queryBuilder.keyword().wildcard().onField( "newsId" ).ignoreFieldBridge().matching(
				"tit*"
		).createQuery();
		@SuppressWarnings("unchecked")
		List<IndexedNews> list = fullTextEm.createFullTextQuery( luceneQuery ).getResultList();
		assertThat( list ).hasSize( 1 );

		List<IndexedLabel> labels = list.get( 0 ).getLabels();
		assertThat( labels ).hasSize( 2 );
		assertThat( contains( labels, "massindex" ) ).isTrue();
		assertThat( contains( labels, "test" ) ).isTrue();
		fullTextEm.getTransaction().commit();
		fullTextEm.close();
	}

	@SuppressWarnings("unchecked")
	private void assertAssociatedElementsHaveBeenIndexed() throws Exception {
		FullTextEntityManager fullTextEm = Search.getFullTextEntityManager( createEntityManager() );
		fullTextEm.getTransaction().begin();
		QueryBuilder b = fullTextEm.getSearchFactory().buildQueryBuilder().forEntity( IndexedLabel.class ).get();
		{
			Query luceneQuery = b.keyword().wildcard().onField( "name" ).matching( "tes*" ).createQuery();
			List<IndexedLabel> labels = fullTextEm.createFullTextQuery( luceneQuery ).getResultList();
			assertThat( labels ).hasSize( 1 );
			assertThat( contains( labels, "test" ) ).isTrue();
		}
		{
			Query luceneQuery = b.keyword().wildcard().onField( "name" ).matching( "mas*" ).createQuery();
			List<IndexedLabel> labels = fullTextEm.createFullTextQuery( luceneQuery ).getResultList();
			assertThat( labels ).hasSize( 1 );
			assertThat( contains( labels, "massindex" ) ).isTrue();
		}
		fullTextEm.getTransaction().commit();
		fullTextEm.close();
	}

	private boolean contains(List<IndexedLabel> list, String label) {
		for ( IndexedLabel indexedLabel : list ) {
			if ( indexedLabel.getName().equals( label ) ) {
				return true;
			}
		}
		return false;
	}

	private EntityManager createEntityManager() {
		return getFactory().createEntityManager();
	}

	private void startAndWaitMassIndexing(Class<?>... entityTypes) throws InterruptedException {
		FullTextEntityManager fullTextEm = Search.getFullTextEntityManager( createEntityManager() );
		fullTextEm.createIndexer( entityTypes ).purgeAllOnStart( true ).startAndWait();
		int numDocs = fullTextEm.getSearchFactory().getIndexReaderAccessor().open( entityTypes ).numDocs();
		fullTextEm.close();
		assertThat( numDocs ).isGreaterThan( 0 );
	}

	private void purgeAll(Class<?>... entityTypes) throws Exception {
		FullTextEntityManager fullTextEm = Search.getFullTextEntityManager( createEntityManager() );
		for ( Class<?> entityType : entityTypes ) {
			fullTextEm.purgeAll( entityType );
			fullTextEm.flushToIndexes();
		}
		int numDocs = fullTextEm.getSearchFactory().getIndexReaderAccessor().open( entityTypes ).numDocs();
		fullTextEm.close();
		assertThat( numDocs ).isEqualTo( 0 );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { IndexedNews.class, IndexedLabel.class };
	}

	@Override
	protected void configure(GetterPersistenceUnitInfo info) {
		super.configure( info );
		info.getProperties().setProperty( "hibernate.search.default.directory_provider", "ram" );
		// Infinispan requires to be set to distribution mode for this test to pass
		info.getProperties().setProperty( "hibernate.ogm.infinispan.configuration_resourcename", "infinispan-dist.xml" );
	}
}
