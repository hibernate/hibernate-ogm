/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.hsearch;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.apache.lucene.search.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.utils.GridDialectType;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.SkipByGridDialect;
import org.hibernate.ogm.utils.TestHelper;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.junit.Test;

/**
 * Verifies basic integration of Hibernate Search works
 * as expected. Indirectly tests transaction synchronizations (OGM-216)
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @author Sanne Grinovero &lt;sanne@hibernate.org&gt; (C) 2012 Red Hat Inc.
 */
@SkipByGridDialect(value = { GridDialectType.NEO4J_EMBEDDED, GridDialectType.NEO4J_REMOTE }, comment = "Neo4j is not compatible with HSEARCH 5")
public class SearchOnStandaloneOGMTest extends OgmTestCase {

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
		final List<Insurance> resultList = ftQuery.list();
		assertThat( resultList ).hasSize( 1 );
		for ( Object e : resultList ) {
			fts.delete( e );
		}
		transaction.commit();
		fts.close();
	}

	@Override
	protected void configure(Map<String, Object> cfg) {
		TestHelper.enableCountersForInfinispan( cfg );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Insurance.class };
	}

}
