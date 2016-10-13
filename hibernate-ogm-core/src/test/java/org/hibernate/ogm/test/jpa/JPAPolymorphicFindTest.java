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
package org.hibernate.ogm.test.jpa;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.test.utils.TestHelper.dropSchemaAndDatabase;
import static org.hibernate.ogm.test.utils.jpa.JpaTestCase.extractJBossTransactionManager;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.transaction.TransactionManager;

import org.hibernate.ogm.test.simpleentity.Hero;
import org.hibernate.ogm.test.simpleentity.SuperHero;
import org.hibernate.ogm.test.utils.PackagingRule;
import org.hibernate.ogm.test.utils.TestHelper;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Jonathan Wood <jonathanshawwood@gmail.com>
 */
public class JPAPolymorphicFindTest {

	@Rule
	public PackagingRule packaging = new PackagingRule( "persistencexml/jpajtastandalone.xml", Hero.class, SuperHero.class );

	@Test
	public void testJPAPolymorphicFind() throws Exception {

		final EntityManagerFactory emf = Persistence.createEntityManagerFactory( "jpajtastandalone", TestHelper.getEnvironmentProperties() );

		TransactionManager transactionManager = extractJBossTransactionManager( emf );

		transactionManager.begin();
		final EntityManager em = emf.createEntityManager();
		Hero h = new Hero();
		h.setName( "Spartacus" );
		em.persist( h );
		SuperHero sh = new SuperHero();
		sh.setName( "Batman" );
		sh.setSpecialPower( "Technology and samurai techniques" );
		em.persist( sh );
		transactionManager.commit();

		em.clear();
		
		transactionManager.begin();
		Hero lh = em.find( Hero.class, h.getName() );
		assertThat( lh ).isNotNull();
		assertThat( lh ).isInstanceOf(Hero.class);
		Hero lsh = em.find( Hero.class, sh.getName() );
		assertThat( lsh ).isNotNull();
		assertThat( lsh ).isInstanceOf(SuperHero.class);
		em.remove( lh );
		em.remove( lsh );

		transactionManager.commit();

		em.close();

		dropSchemaAndDatabase( emf );
		emf.close();
	}

}

