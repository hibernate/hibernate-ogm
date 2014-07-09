/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.hsearch;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import org.apache.lucene.search.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.query.DatabaseRetrievalMethod;
import org.hibernate.search.query.ObjectLookupMethod;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.junit.Test;

/**
 * Verifies basic integration of Hibernate Search works
 * as expected. Indirectly tests transaction synchronizations (OGM-216)
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @author Sanne Grinovero &lt;sanne@hibernate.org&gt; (C) 2012 Red Hat Inc.
 */
public class SearchOnStandaloneOGMTest extends OgmTestCase {

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( "hibernate.search.default.directory_provider", "ram" );
	}

	@Test
	public void testHibernateSearchJPAAPIUsage() throws Exception {
		final Session session = openSession();
		Transaction transaction = session.beginTransaction();
		final FullTextSession fts = Search.getFullTextSession( session );
		final Insurance insurance = new Insurance();
		insurance.setName( "Macif" );
		fts.persist( insurance );
		transaction.commit();

		fts.clear();

		transaction = fts.beginTransaction();
		final QueryBuilder b = fts.getSearchFactory()
				.buildQueryBuilder()
				.forEntity( Insurance.class )
				.get();
		final Query lq = b.keyword().onField( "name" ).matching( "Macif" ).createQuery();
		final FullTextQuery ftQuery = fts.createFullTextQuery( lq, Insurance.class );
		ftQuery.initializeObjectsWith( ObjectLookupMethod.SKIP, DatabaseRetrievalMethod.FIND_BY_ID );
		final List<Insurance> resultList = ftQuery.list();
		assertThat( resultList ).hasSize( 1 );
		for ( Object e : resultList ) {
			fts.delete( e );
		}
		transaction.commit();
		fts.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Insurance.class };
	}

}
