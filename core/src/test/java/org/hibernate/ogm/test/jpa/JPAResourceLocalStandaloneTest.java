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
package org.hibernate.ogm.test.jpa;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.test.utils.TestHelper.dropSchemaAndDatabase;
import static org.hibernate.ogm.test.utils.TestHelper.assertNumberOfEntities;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import org.hibernate.ogm.test.utils.PackagingRule;
import org.hibernate.ogm.test.utils.RequiresTransactionalCapabilitiesRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class JPAResourceLocalStandaloneTest {

	@Rule
	public PackagingRule packaging = new PackagingRule( "persistencexml/jpajtastandalone-resourcelocal.xml", Poem.class );

	@Rule
	public RequiresTransactionalCapabilitiesRule transactions = new RequiresTransactionalCapabilitiesRule();

	@Test
	public void testJTAStandalone() throws Exception {
		final EntityManagerFactory emf = Persistence.createEntityManagerFactory( "jpajtastandalone" );
		try {

			final EntityManager em = emf.createEntityManager();
			try {

				em.getTransaction().begin();
				Poem poem = new Poem();
				poem.setName( "L'albatros" );
				em.persist( poem );
				em.getTransaction().commit();

				em.clear();

				em.getTransaction().begin();
				Poem poem2 = new Poem();
				poem2.setName( "Wazaaaaa" );
				em.persist( poem2 );
				em.flush();
				assertThat( assertNumberOfEntities( 2, em ) ).isTrue();
				em.getTransaction().rollback();

				assertThat( assertNumberOfEntities( 1, em ) ).isTrue();

				em.getTransaction().begin();
				poem = em.find( Poem.class, poem.getId() );
				assertThat( poem ).isNotNull();
				assertThat( poem.getName() ).isEqualTo( "L'albatros" );
				em.remove( poem );
				poem2 = em.find( Poem.class, poem2.getId() );
				assertThat( poem2 ).isNull();
				em.getTransaction().commit();

			}
			finally {
				EntityTransaction transaction = em.getTransaction();
				if ( transaction != null && transaction.isActive() ) {
					transaction.rollback();
				}
				em.close();
			}
		}
		finally {
			dropSchemaAndDatabase( emf );
			emf.close();
		}
	}


}
