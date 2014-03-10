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
package org.hibernate.ogm.datastore.neo4j.test.query.nativequery;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.datastore.neo4j.test.query.nativequery.OscarWildePoem.TABLE_NAME;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.utils.OgmTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 *  Test the execution of native queries on MongoDB using the {@link Session}
 *
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class Neo4jSessionSQLQueryTest extends OgmTestCase {

	private final OscarWildePoem portia = new OscarWildePoem( 1L, "Portia", "Oscar Wilde" );
	private final OscarWildePoem athanasia = new OscarWildePoem( 2L, "Athanasia", "Oscar Wilde" );

	@Before
	public void init() {
		Session session = openSession();
		Transaction transaction = session.beginTransaction();
		session.persist( portia );
		session.persist( athanasia );
		transaction.commit();
		session.clear();
		session.close();
	}

	@After
	public void tearDown() {
		Session session = openSession();
		Transaction tx = session.beginTransaction();
		delete( session, portia );
		delete( session, athanasia );
		tx.commit();
		session.clear();
		session.close();
	}

	private void delete(Session session, OscarWildePoem poem) {
		Object entity = session.get( OscarWildePoem.class, poem.getId() );
		if ( entity != null ) {
			session.delete( entity );
		}
	}

	@Test
	public void testUniqueResultQuery() throws Exception {
		Session session = openSession();
		Transaction transaction = session.beginTransaction();

		String nativeQuery = "MATCH ( n:" + TABLE_NAME + " { name:'Portia', author:'Oscar Wilde' } ) RETURN n";
		OscarWildePoem poem = (OscarWildePoem) session.createSQLQuery( nativeQuery )
				.addEntity( OscarWildePoem.TABLE_NAME, OscarWildePoem.class )
				.uniqueResult();

		assertAreEquals( portia, poem );

		transaction.commit();
		session.clear();
		session.close();
	}

	@Test
	public void testListMultipleResultQuery() throws Exception {
		Session session = openSession();
		Transaction transaction = session.beginTransaction();

		String nativeQuery = "MATCH ( n:" + TABLE_NAME + " ) RETURN n ORDER BY n.name";
		@SuppressWarnings("unchecked")
		List<OscarWildePoem> result = session.createSQLQuery( nativeQuery )
				.addEntity( OscarWildePoem.TABLE_NAME, OscarWildePoem.class )
				.list();

		assertThat( result ).as( "Unexpected number of results" ).hasSize( 2 );
		assertAreEquals( athanasia, result.get( 0 ) );
		assertAreEquals( portia, result.get( 1 ) );

		transaction.commit();
		session.clear();
		session.close();
	}

	@Test
	public void testListMultipleResultQueryWithoutReturnedType() throws Exception {
		Session session = openSession();
		Transaction transaction = session.beginTransaction();

		String nativeQuery = "MATCH ( n:" + TABLE_NAME + " ) RETURN n.name, n.author ORDER BY n.name";
		@SuppressWarnings("unchecked")
		List<Object[]> result = session.createSQLQuery( nativeQuery ).list();

		assertThat( result ).as( "Unexpected number of results" ).hasSize( 2 );

		Object[] athanasiaRow = result.get( 0 );
		assertThat( athanasiaRow[0] ).isEqualTo( athanasia.getName() );
		assertThat( athanasiaRow[1] ).isEqualTo( athanasia.getAuthor() );

		Object[] portiaRow = result.get( 1 );
		assertThat( portiaRow[0] ).isEqualTo( portia.getName() );
		assertThat( portiaRow[1] ).isEqualTo( portia.getAuthor() );

		transaction.commit();
		session.clear();
		session.close();
	}

	@Test
	public void testUniqueResultNamedNativeQuery() throws Exception {
		Session session = openSession();
		Transaction transaction = session.beginTransaction();

		try {
			OscarWildePoem uniqueResult = (OscarWildePoem) session.getNamedQuery( "AthanasiaQuery" )
					.uniqueResult();
			assertAreEquals( uniqueResult, athanasia );
			transaction.commit();
		}
		finally {
			session.clear();
			session.close();
		}
	}

	private void assertAreEquals(OscarWildePoem expectedPoem, OscarWildePoem poem) {
		assertThat( poem ).isNotNull();
		assertThat( poem.getId() ).as( "Wrong Id" ).isEqualTo( expectedPoem.getId() );
		assertThat( poem.getName() ).as( "Wrong Name" ).isEqualTo( expectedPoem.getName() );
		assertThat( poem.getAuthor() ).as( "Wrong Author" ).isEqualTo( expectedPoem.getAuthor() );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { OscarWildePoem.class };
	}

}
