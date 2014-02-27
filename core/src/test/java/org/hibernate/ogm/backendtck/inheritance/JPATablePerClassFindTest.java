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
package org.hibernate.ogm.backendtck.inheritance;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.utils.TestHelper.dropSchemaAndDatabase;
import static org.hibernate.ogm.utils.jpa.JpaTestCase.extractJBossTransactionManager;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.transaction.TransactionManager;

import org.hibernate.ogm.utils.PackagingRule;
import org.hibernate.ogm.utils.TestHelper;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class JPATablePerClassFindTest {

	@Rule
	public PackagingRule packaging = new PackagingRule( "persistencexml/jpajtastandalone.xml", CommunityMember.class, Employee.class );

	@Test
	public void testJPAPolymorphicFind() throws Exception {
		final EntityManagerFactory emf = Persistence.createEntityManagerFactory( "jpajtastandalone",
				TestHelper.getEnvironmentProperties() );
		TransactionManager tm = extractJBossTransactionManager( emf );

		tm.begin();
		final EntityManager em = emf.createEntityManager();
		CommunityMember member = new CommunityMember( "Davide" );
		em.persist( member );

		Employee employee = new Employee( "Alex", "EMPLOYER" );
		em.persist( employee );
		tm.commit();

		em.clear();

		tm.begin();
		CommunityMember lh = em.find( CommunityMember.class, member.name );
		assertThat( lh ).isNotNull();
		assertThat( lh ).isInstanceOf( CommunityMember.class );

		CommunityMember lsh = em.find( Employee.class, employee.name );
		assertThat( lsh ).isNotNull();
		assertThat( lsh ).isInstanceOf( Employee.class );
		assertThat( ((Employee) employee).employer ).isEqualTo( employee.employer );

		em.remove( lh );
		em.remove( lsh );
		tm.commit();
		em.close();
		dropSchemaAndDatabase( emf );
		emf.close();
	}

}
