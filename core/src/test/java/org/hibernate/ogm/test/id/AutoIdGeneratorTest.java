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
package org.hibernate.ogm.test.id;

import javax.persistence.EntityManager;

import org.junit.Test;

import org.hibernate.ogm.test.utils.jpa.JpaTestCase;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Test case for JPA Auto identifier generator.
 *
 * @author Nabeel Ali Memon <nabeel@nabeelalimemon.com>
 */
public class AutoIdGeneratorTest extends JpaTestCase {

	@Test
	public void testAutoIdentifierGenerator() throws Exception {
		DistributedRevisionControl git = new DistributedRevisionControl();
		DistributedRevisionControl bzr = new DistributedRevisionControl();
		getTransactionManager().begin();
		final EntityManager em = getFactory().createEntityManager();
		boolean operationSuccessfull = false;
		try {
			git.setName( "Git" );
			em.persist( git );

			bzr.setName( "Bazaar" );
			em.persist( bzr );
			operationSuccessfull = true;
		}
		finally {
			commitOrRollback( operationSuccessfull );
		}

		em.clear();
		getTransactionManager().begin();
		operationSuccessfull = false;
		try {
			DistributedRevisionControl dvcs = em.find( DistributedRevisionControl.class, git.getId() );
			assertThat( dvcs ).isNotNull();
			assertThat( dvcs.getId() ).isEqualTo( 1 );
			em.remove( dvcs );

			dvcs = em.find( DistributedRevisionControl.class, bzr.getId() );
			assertThat( dvcs ).isNotNull();
			assertThat( dvcs.getId() ).isEqualTo( 2 );
			operationSuccessfull = true;
		}
		finally {
			commitOrRollback( operationSuccessfull );
		}
		em.close();
	}

	@Override
	public Class<?>[] getEntities() {
		return new Class<?>[] {
				DistributedRevisionControl.class
		};
	}
}
