/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
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

import java.util.List;

import javax.persistence.EntityManager;

import org.apache.lucene.search.Query;
import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.ogm.test.utils.jpa.GetterPersistenceUnitInfo;
import org.hibernate.ogm.test.utils.jpa.JpaTestCase;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.DatabaseRetrievalMethod;
import org.hibernate.search.query.ObjectLookupMethod;
import org.hibernate.search.query.dsl.QueryBuilder;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
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
