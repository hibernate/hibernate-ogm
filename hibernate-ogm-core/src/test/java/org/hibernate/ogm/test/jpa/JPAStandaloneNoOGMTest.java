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

import org.hibernate.ogm.test.utils.PackagingRule;
import org.junit.Rule;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.transaction.Status;
import javax.transaction.TransactionManager;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.test.utils.jpa.JpaTestCase.extractJBossTransactionManager;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class JPAStandaloneNoOGMTest {

	@Rule
	public PackagingRule packaging = new PackagingRule( "persistencexml/jpajtastandalone-noogm.xml", Poem.class );

	@Test
	public void testJTAStandaloneNoOgm() throws Exception {
		EntityManagerFactory emf = null;
		boolean emfBuilt = false;
		emf = Persistence.createEntityManagerFactory( "jpajtastandalone-noogm" );

		TransactionManager transactionManager = extractJBossTransactionManager( emf );
		try {


			transactionManager.begin();
			final EntityManager em = emf.createEntityManager();
			Poem poem = new Poem();
			poem.setName( "L'albatros" );
			em.persist( poem );
			transactionManager.commit();

			em.clear();

			transactionManager.begin();
			poem = em.find( Poem.class, poem.getId() );
			assertThat( poem ).isNotNull();
			assertThat( poem.getName() ).isEqualTo( "L'albatros" );
			em.remove( poem );
			transactionManager.commit();

			em.close();

			emfBuilt = true;
		}
		catch (Exception e) {
			// e.printStackTrace(  );
		}
		finally {
			if ( transactionManager.getStatus() == Status.STATUS_ACTIVE ) {
				transactionManager.rollback();
			}
		}
		if ( emfBuilt ) {
			emf.close();
		}
		assertThat( emfBuilt ).as( "Should fail as we don't have a RDBMS" ).isFalse();
	}


}
