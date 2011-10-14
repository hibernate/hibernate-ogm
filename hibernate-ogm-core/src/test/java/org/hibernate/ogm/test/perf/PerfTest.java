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
		int nbrOfLoop = 10000;
		System.out.printf("Warming up\n");
		for (int j = 0 ; j < 200 ; j++) {
			save200AuthorsAndCommit(em);
		}

		System.out.printf( "Warm up period done\nSaving %s entities\n", nbrOfLoop);
		long start = System.nanoTime();

		for (int j = 0 ; j < nbrOfLoop; j++) {
			save200AuthorsAndCommit(em);
		}
		System.out.printf("Saving %s took %sms ie %sns/entry\n", 200*nbrOfLoop, (System.nanoTime() - start) / 1000000, (System.nanoTime() - start)/(200*nbrOfLoop));
		em.close();
		getTransactionManager().commit();

		getTransactionManager().begin();
		em = getFactory().createEntityManager();
		int nbr_of_reads = 100000;
		start = System.nanoTime();
		for (int i = 0 ; i < nbr_of_reads; i++) {
			int primaryKey = rand.nextInt(nbrOfLoop-1)+1; //start from 1
			Author author = em.find(Author.class, primaryKey);
			if ( author == null ) {
				System.out.printf("Cannot find author %s, %sth loop\n", primaryKey, i);
			}
			else {
				assertThat(author.getBio()).isNotEmpty();
				assertThat(author.getA_id()).isEqualTo(primaryKey);
			}
		}
		System.out.printf("Reading %s took %sms ie %sns/entry\n", nbr_of_reads, (System.nanoTime() - start) / 1000000, (System.nanoTime() - start)/(nbr_of_reads));

		em.close();
		getTransactionManager().commit();
	}

	private void save200AuthorsAndCommit(EntityManager em) throws Exception {
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

	@Override
	public Class<?>[] getEntities() {
		return new Class<?>[] {
				Author.class,
				Blog.class
		};
	}
}
