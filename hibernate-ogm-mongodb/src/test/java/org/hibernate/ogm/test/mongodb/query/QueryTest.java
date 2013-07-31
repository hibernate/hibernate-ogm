/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.test.mongodb.query;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.ogm.cfg.OgmConfiguration;
import org.hibernate.ogm.dialect.mongodb.query.parsing.MongoDBBasedQueryParserService;
import org.hibernate.ogm.test.utils.OgmTestCase;
import org.hibernate.ogm.test.utils.TestSessionFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test for simple queries with MongoDB.
 *
 * @author Gunnar Morling
 */
public class QueryTest extends OgmTestCase {

	@TestSessionFactory
	private static SessionFactory sessions;

	private Session session;
	private Transaction transaction;

	@BeforeClass
	public static void addTestEntities() {
		Session session = sessions.openSession();
		Transaction transaction = session.getTransaction();
		transaction.begin();

		Hypothesis hypothesis = new Hypothesis();
		hypothesis.setId( "1" );
		hypothesis.setPosition( 1 );
		hypothesis.setDescription( "Alea iacta est" );
		session.persist( hypothesis );

		hypothesis = new Hypothesis();
		hypothesis.setId( "2" );
		hypothesis.setPosition( 2 );
		hypothesis.setDescription( "Cave canem" );
		session.persist( hypothesis );

		transaction.commit();
		session.clear();
		session.close();
	}

	@AfterClass
	public static void deleteTestEntities() throws Exception {
		Session session = sessions.openSession();
		Transaction transaction = session.getTransaction();
		transaction.begin();

		session.delete( new Hypothesis( "1" ) );
		session.delete( new Hypothesis( "2" ) );

		transaction.commit();
		session.clear();
		session.close();
	}

	@Before
	public void startTransaction() {
		session = sessions.openSession();
		transaction = session.getTransaction();
		transaction.begin();
	}

	@After
	public void commitTransaction() {
		session.close();
		transaction.commit();
	}

	@Test
	public void shouldReturnAllObjectsForUnrestrictedQuery() throws Exception {
		@SuppressWarnings("unchecked")
		List<Hypothesis> results = session.createQuery( "from Hypothesis" ).list();
		assertThat( results ).onProperty( "id" ).containsOnly( "1", "2" );
	}

	@Test
	public void shouldReturnSingleEntityForId() throws Exception {
		@SuppressWarnings("unchecked")
		List<Hypothesis> results = session.createQuery( "from Hypothesis h where h.id = '2'" ).list();
		assertThat( results ).onProperty( "id" ).containsOnly( "2" );
	}

	@Test
	public void shouldApplyCorrectColumnName() throws Exception {
		@SuppressWarnings("unchecked")
		List<Hypothesis> results = session.createQuery( "from Hypothesis h where h.position = '2'" ).list();
		assertThat( results ).onProperty( "id" ).containsOnly( "2" );
	}

	@Test
	public void shouldApplyConjunctionPredicate() throws Exception {
		@SuppressWarnings("unchecked")
		List<Hypothesis> results = session.createQuery( "from Hypothesis h where h.id = '1' and h.position = '1'" ).list();
		assertThat( results ).onProperty( "id" ).containsOnly( "1" );
	}

	@Test
	public void shouldApplyNegationPredicate() throws Exception {
		@SuppressWarnings("unchecked")
		List<Hypothesis> results = session.createQuery( "from Hypothesis h where h.id <> '1'" ).list();
		assertThat( results ).onProperty( "id" ).containsOnly( "2" );
	}

	@Test
	public void shouldReturnObjectArrayFromProjectionQuery() throws Exception {
		@SuppressWarnings("unchecked")
		List<Object> results = session.createQuery( "select h.id, h.position from Hypothesis h where h.position = 2" ).list();

		assertThat( results ).hasSize( 1 );
		assertThat( (Object[]) results.iterator().next() ).isEqualTo( new Object[] { "2", 2 } );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Hypothesis.class };
	}

	@Override
	protected void configure(Configuration cfg) {
		cfg.setProperty( OgmConfiguration.OGM_QUERY_PARSER_SERVICE, MongoDBBasedQueryParserService.class.getCanonicalName() );
	}
}
