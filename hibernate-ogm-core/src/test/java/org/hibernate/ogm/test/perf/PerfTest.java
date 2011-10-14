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
package org.hibernate.ogm.test.perf;

import org.hibernate.ogm.test.jpa.util.JpaTestCase;
import org.junit.Test;

import javax.persistence.EntityManager;
import java.util.Date;
import java.util.Random;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class PerfTest extends JpaTestCase {

	private static Random rand = new Random();

	public static void main(String[] args) {
		PerfTest perfTest = new PerfTest();
		try {
			perfTest.createFactory();
			perfTest.testSimpleEntityInserts();
			perfTest.closeFactory();
		}
		catch (Exception e) {
			e.printStackTrace();
		}

	}


	public void testSimpleEntityInserts() throws Exception {
		getTransactionManager().begin();
		EntityManager em = getFactory().createEntityManager();
		for (int j = 0 ; j < 20000 ; j++) {
			System.out.println("Adding 200 authors");
			for (int i = 0; i < 200 ; i++) {
				Author author = new Author();
				author.setBio("This is a decent size bio made of " + rand.nextDouble() + " stuffs");
				author.setDob(new Date());
				author.setFname("Emmanuel " + rand.nextInt());
				author.setLname("Bernard " + rand.nextInt());
				author.setMname("" + rand.nextInt(26));
				em.persist(author);
			}
			em.flush();
			getTransactionManager().commit();
			em.clear();
			getTransactionManager().begin();
			em.joinTransaction();
		}
		em.close();
		getTransactionManager().commit();

		getTransactionManager().begin();
		em = getFactory().createEntityManager();
		System.out.println("Reading 100 authors");
		for (int i = 0 ; i < 100 ; i++) {
			int primaryKey = rand.nextInt(999) + 1;
			Author author = em.find(Author.class, primaryKey);
			assertThat(author.getBio()).isNotEmpty();
			assertThat(author.getA_id()).isEqualTo(primaryKey);
		}
		em.close();
		getTransactionManager().commit();
	}

	@Override
	public Class<?>[] getEntities() {
		return new Class<?>[] {
				Author.class
		};
	}
}
