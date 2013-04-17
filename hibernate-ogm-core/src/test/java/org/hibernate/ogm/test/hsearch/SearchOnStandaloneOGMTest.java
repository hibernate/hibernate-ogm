/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.test.hsearch;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import org.apache.lucene.search.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.ogm.test.simpleentity.OgmTestCase;
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
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
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
