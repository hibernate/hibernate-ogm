/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.hsearch;

import java.util.List;

import javax.persistence.EntityManager;

import org.apache.lucene.search.Query;
import org.junit.Test;
import org.hibernate.Session;
import org.hibernate.ogm.utils.jpa.GetterPersistenceUnitInfo;
import org.hibernate.ogm.utils.jpa.JpaTestCase;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.DatabaseRetrievalMethod;
import org.hibernate.search.query.ObjectLookupMethod;
import org.hibernate.search.query.dsl.QueryBuilder;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class HibernateSearchAtopOgmTest extends JpaTestCase {

	@Test
	public void testHibernateSearchJPAAPIUsage() throws Exception {
		getTransactionManager().begin();
		final FullTextEntityManager ftem = Search.getFullTextEntityManager( getFactory().createEntityManager() );
		final Insurance insurance = new Insurance();
		insurance.setName( "Macif" );
		ftem.persist( insurance );
		getTransactionManager().commit();

		ftem.clear();

		getTransactionManager().begin();
		final QueryBuilder b = ftem.getSearchFactory()
				.buildQueryBuilder()
				.forEntity( Insurance.class )
				.get();
		final Query lq = b.keyword().onField( "name" ).matching( "Macif" ).createQuery();
		final FullTextQuery ftQuery = ftem.createFullTextQuery( lq, Insurance.class );
		ftQuery.initializeObjectsWith( ObjectLookupMethod.SKIP, DatabaseRetrievalMethod.FIND_BY_ID );
		final List<Insurance> resultList = ftQuery.getResultList();
		assertThat( getFactory().getPersistenceUnitUtil().isLoaded( resultList.get( 0 ) ) ).isTrue();
		assertThat( resultList ).hasSize( 1 );
		for ( Object e : resultList ) {
			ftem.remove( e );
		}
		getTransactionManager().commit();
		ftem.close();
	}

	@Test
	public void testHibernateSearchNativeAPIUsage() throws Exception {
		getTransactionManager().begin();
		final EntityManager entityManager = getFactory().createEntityManager();
		final FullTextSession ftSession = org.hibernate.search.Search.getFullTextSession( entityManager.unwrap( Session.class ) );
		final Insurance insurance = new Insurance();
		insurance.setName( "Macif" );
		ftSession.persist( insurance );
		getTransactionManager().commit();

		ftSession.clear();

		getTransactionManager().begin();
		final QueryBuilder b = ftSession.getSearchFactory()
				.buildQueryBuilder()
				.forEntity( Insurance.class )
				.get();
		final Query lq = b.keyword().onField( "name" ).matching( "Macif" ).createQuery();
		final org.hibernate.search.FullTextQuery ftQuery = ftSession.createFullTextQuery( lq, Insurance.class );
		ftQuery.initializeObjectsWith( ObjectLookupMethod.SKIP, DatabaseRetrievalMethod.FIND_BY_ID );
		final List<Insurance> resultList = ftQuery.list();
		assertThat( getFactory().getPersistenceUnitUtil().isLoaded( resultList.get( 0 ) ) ).isTrue();
		assertThat( resultList ).hasSize( 1 );
		for ( Object e : resultList ) {
			ftSession.delete( e );
		}
		getTransactionManager().commit();
		entityManager.close();
	}

	@Override
	protected void refineInfo(GetterPersistenceUnitInfo info) {
		super.refineInfo( info );
		info.getProperties().setProperty( "hibernate.search.default.directory_provider", "ram" );
	}

	@Override
	public Class<?>[] getEntities() {
		return new Class<?>[] { Insurance.class };
	}
}
