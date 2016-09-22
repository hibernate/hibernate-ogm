/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.hsearch;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import javax.persistence.EntityManager;

import org.apache.lucene.search.Query;
import org.hibernate.Session;
import org.hibernate.ogm.utils.GridDialectType;
import org.hibernate.ogm.utils.SkipByGridDialect;
import org.hibernate.ogm.utils.jpa.OgmJpaTestCase;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.junit.Test;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
@SkipByGridDialect(value = { GridDialectType.NEO4J_EMBEDDED, GridDialectType.NEO4J_REMOTE }, comment = "Neo4j is not compatible with HSEARCH 5")
public class HibernateSearchAtopOgmTest extends OgmJpaTestCase {

	@Test
	public void testHibernateSearchJPAAPIUsage() throws Exception {
		final FullTextEntityManager ftem = Search.getFullTextEntityManager( getFactory().createEntityManager() );
		ftem.getTransaction().begin();
		final Insurance insurance = new Insurance();
		insurance.setName( "Macif" );
		ftem.persist( insurance );
		ftem.getTransaction().commit();

		ftem.clear();

		ftem.getTransaction().begin();
		final QueryBuilder b = ftem.getSearchFactory()
				.buildQueryBuilder()
				.forEntity( Insurance.class )
				.get();
		final Query lq = b.keyword().onField( "name" ).matching( "Macif" ).createQuery();
		final FullTextQuery ftQuery = ftem.createFullTextQuery( lq, Insurance.class );
		final List<Insurance> resultList = ftQuery.getResultList();
		assertThat( getFactory().getPersistenceUnitUtil().isLoaded( resultList.get( 0 ) ) ).isTrue();
		assertThat( resultList ).hasSize( 1 );
		for ( Object e : resultList ) {
			ftem.remove( e );
		}
		ftem.getTransaction().commit();
		ftem.close();
	}

	@Test
	public void testHibernateSearchNativeAPIUsage() throws Exception {
		final EntityManager entityManager = getFactory().createEntityManager();
		final FullTextSession ftSession = org.hibernate.search.Search.getFullTextSession( entityManager.unwrap( Session.class ) );
		entityManager.getTransaction().begin();
		final Insurance insurance = new Insurance();
		insurance.setName( "Macif" );
		ftSession.persist( insurance );
		entityManager.getTransaction().commit();

		ftSession.clear();

		entityManager.getTransaction().begin();
		final QueryBuilder b = ftSession.getSearchFactory()
				.buildQueryBuilder()
				.forEntity( Insurance.class )
				.get();
		final Query lq = b.keyword().onField( "name" ).matching( "Macif" ).createQuery();
		final org.hibernate.search.FullTextQuery ftQuery = ftSession.createFullTextQuery( lq, Insurance.class );
		final List<Insurance> resultList = ftQuery.list();
		assertThat( getFactory().getPersistenceUnitUtil().isLoaded( resultList.get( 0 ) ) ).isTrue();
		assertThat( resultList ).hasSize( 1 );
		for ( Object e : resultList ) {
			ftSession.delete( e );
		}
		entityManager.getTransaction().commit();
		entityManager.close();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Insurance.class };
	}
}
