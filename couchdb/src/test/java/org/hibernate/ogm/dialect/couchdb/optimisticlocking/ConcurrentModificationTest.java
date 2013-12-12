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
package org.hibernate.ogm.dialect.couchdb.optimisticlocking;

import static org.fest.assertions.Assertions.assertThat;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import org.hibernate.Session;
import org.hibernate.StaleObjectStateException;
import org.hibernate.Transaction;
import org.hibernate.ogm.dialect.couchdb.type.CouchDBStringType;
import org.hibernate.ogm.test.utils.OgmTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for concurrent updates of CouchDB entities with mapped revision property.
 *
 * @author Gunnar Morling
 */
public class ConcurrentModificationTest extends OgmTestCase {

	private Session session;

	@Before
	public void createSession() {
		session = openSession();
	}

	@After
	public void deleteTestDataAndCloseSession() {
		session.clear();
		if ( session.getTransaction().isActive() ) {
			session.getTransaction().rollback();
		}
		Transaction transaction = session.beginTransaction();

		Hypothesis hypothesis = (Hypothesis) session.get( Hypothesis.class, "hypothesis-1" );
		if ( hypothesis != null ) {
			session.delete( hypothesis );
		}

		Animal animal = (Animal) session.get( Animal.class, "animal-1" );
		if ( animal != null ) {
			session.delete( animal );
		}

		transaction.commit();
		session.close();
	}

	@Test(expected = StaleObjectStateException.class)
	public void concurrentModificationShouldCauseException() throws Exception {
		Hypothesis hypothesis = createAndPersistHypothesis();

		String newRevision = doConcurrentUpdateToHypothesis();
		assertThat( newRevision ).isNotEqualTo( hypothesis.get_rev() );

		Transaction transaction = session.beginTransaction();
		hypothesis.setDescription( "Description 2" );
		transaction.commit();
	}

	@Test(expected = StaleObjectStateException.class)
	public void mergeAfterConcurrentModificationShouldCauseException() throws Exception {
		Hypothesis hypothesis = createAndPersistHypothesis();
		session.clear();

		doConcurrentUpdateToHypothesis();

		session.beginTransaction();
		hypothesis = (Hypothesis) session.merge( hypothesis );
	}

	@Test(expected = StaleObjectStateException.class)
	public void deletionAfterConcurrentModificationShouldCauseException() throws Exception {
		Hypothesis hypothesis = createAndPersistHypothesis();

		doConcurrentUpdateToHypothesis();

		Transaction transaction = session.beginTransaction();
		session.delete( hypothesis );
		transaction.commit();
	}

	private Hypothesis createAndPersistHypothesis() {
		Transaction transaction = session.beginTransaction();

		Hypothesis hypothesis = createHypothesis();

		assertThat( hypothesis.get_rev() ).isNull();
		session.persist( hypothesis );
		transaction.commit();
		assertThat( hypothesis.get_rev() ).isNotNull();
		return hypothesis;
	}

	private Hypothesis createHypothesis() {
		Hypothesis hypothesis = new Hypothesis();
		hypothesis.setId( "hypothesis-1" );
		hypothesis.setDescription( "Description 1" );
		hypothesis.setPosition( 1 );

		return hypothesis;
	}

	private String doConcurrentUpdateToHypothesis() throws Exception {
		return Executors.newSingleThreadExecutor().submit( new Callable<String>() {

			@Override
			public String call() throws Exception {
				Session session = openSession();

				Transaction transaction = session.beginTransaction();
				final Hypothesis hypothesis = (Hypothesis) session.get( Hypothesis.class, "hypothesis-1" );
				hypothesis.setDescription( "Description 2" );
				transaction.commit();

				return hypothesis.get_rev();
			}
		} ).get();
	}

	@Test(expected = StaleObjectStateException.class)
	public void customColumnNameShouldBeUsableForRevisionProperty() throws Exception {
		Animal animal = createAndPersistAnimal();

		String newRevision = doConcurrentUpdateToAnimal();
		assertThat( newRevision ).isNotEqualTo( animal.getRevision() );

		Transaction transaction = session.beginTransaction();
		animal.setName( "Xavier" );
		transaction.commit();
	}

	private Animal createAndPersistAnimal() {
		Animal animal = new Animal();
		animal.setId( "animal-1" );
		animal.setName( "Bruno" );

		Transaction transaction = session.beginTransaction();
		assertThat( animal.getRevision() ).isNull();
		session.persist( animal );
		transaction.commit();
		assertThat( animal.getRevision() ).isNotNull();

		return animal;
	}

	private String doConcurrentUpdateToAnimal() throws Exception {
		return Executors.newSingleThreadExecutor().submit( new Callable<String>() {

			@Override
			public String call() throws Exception {
				Session session = openSession();

				Transaction transaction = session.beginTransaction();
				final Animal animal = (Animal) session.get( Animal.class, "animal-1" );
				animal.setName( "Xavier" );
				transaction.commit();

				return animal.getRevision();
			}
		} ).get();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Hypothesis.class, Animal.class };
	}

	@Override
	protected void configure(org.hibernate.cfg.Configuration cfg) {
		cfg.registerTypeOverride( new CouchDBStringType() );
	};
}
