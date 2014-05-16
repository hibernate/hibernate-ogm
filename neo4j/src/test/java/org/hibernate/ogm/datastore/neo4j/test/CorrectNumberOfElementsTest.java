/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011-2014 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.datastore.neo4j.test;

import static org.fest.assertions.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import org.hibernate.ogm.backendtck.associations.collection.manytomany.AccountOwner;
import org.hibernate.ogm.backendtck.associations.collection.manytomany.BankAccount;
import org.hibernate.ogm.backendtck.associations.collection.types.Child;
import org.hibernate.ogm.backendtck.associations.collection.types.Father;
import org.hibernate.ogm.backendtck.associations.collection.unidirectional.Cloud;
import org.hibernate.ogm.backendtck.associations.collection.unidirectional.SnowFlake;
import org.hibernate.ogm.backendtck.associations.manytoone.Beer;
import org.hibernate.ogm.backendtck.associations.manytoone.Brewery;
import org.hibernate.ogm.backendtck.associations.manytoone.JUG;
import org.hibernate.ogm.backendtck.associations.manytoone.Member;
import org.hibernate.ogm.backendtck.associations.manytoone.SalesForce;
import org.hibernate.ogm.backendtck.associations.manytoone.SalesGuy;
import org.hibernate.ogm.backendtck.embeddable.Account;
import org.hibernate.ogm.backendtck.embeddable.Address;
import org.hibernate.ogm.backendtck.embeddable.MultiAddressAccount;
import org.hibernate.ogm.backendtck.id.DistributedRevisionControl;
import org.hibernate.ogm.backendtck.id.Label;
import org.hibernate.ogm.backendtck.id.News;
import org.hibernate.ogm.backendtck.id.NewsID;
import org.hibernate.ogm.utils.jpa.JpaTestCase;
import org.junit.After;
import org.junit.Test;

/**
 * These tests only purpose is to check the the right number of nodes and relationships is created in the different
 * scenarios.
 *
 * @author Davide D'Alto
 */
public class CorrectNumberOfElementsTest extends JpaTestCase {

	@Test
	public void testSingleEntityCreationWithoutSequences() throws Exception {
		getTransactionManager().begin();
		final EntityManager em = getFactory().createEntityManager();
		JUG jug = new JUG();
		jug.setName( "JUG Summer Camp" );
		em.persist( jug );
		commitOrRollback( true );
		em.close();

		assertNumberOfNodes( 1 );
		assertRelationships( 0 );
	}

	@Test
	public void testSingleEntityCreationWithSequences() throws Exception {
		getTransactionManager().begin();
		final EntityManager em = getFactory().createEntityManager();
		DistributedRevisionControl git = new DistributedRevisionControl();
		git.setName( "GIT" );
		em.persist( git );
		commitOrRollback( true );
		em.close();

		// 1 node for the sequence + 1 node for the entity
		assertNumberOfNodes( 2 );
		assertRelationships( 0 );
	}

	@Test
	public void testBidirectionalManyToMany() throws Exception {
		getTransactionManager().begin();
		final EntityManager em = getFactory().createEntityManager();

		AccountOwner owner = new AccountOwner();
		owner.setSSN( "0123456" );

		BankAccount soge = new BankAccount();
		soge.setAccountNumber( "X2345000" );
		soge.getOwners().add( owner );
		owner.getBankAccounts().add( soge );

		BankAccount barclays = new BankAccount();
		barclays.setAccountNumber( "ZZZ-009" );
		barclays.getOwners().add( owner );
		owner.getBankAccounts().add( barclays );

		em.persist( owner );
		commitOrRollback( true );
		em.close();

		assertNumberOfNodes( 3 );
		assertRelationships( 4 );
	}

	@Test
	public void testUnidirectionalManyToOne() throws Exception {
		getTransactionManager().begin();
		final EntityManager em = getFactory().createEntityManager();

		JUG jug = new JUG();
		jug.setName( "JUG Summer Camp" );
		em.persist( jug );

		Member emmanuel = new Member();
		emmanuel.setName( "Emmanuel Bernard" );
		emmanuel.setMemberOf( jug );

		Member jerome = new Member();
		jerome.setName( "Jerome" );
		jerome.setMemberOf( jug );

		em.persist( emmanuel );
		em.persist( jerome );
		commitOrRollback( true );
		em.close();

		assertNumberOfNodes( 3 );
		assertRelationships( 0 );
	}

	@Test
	public void testUnidirectionalCollection() throws Exception {
		getTransactionManager().begin();
		final EntityManager em = getFactory().createEntityManager();

		SnowFlake sf = new SnowFlake();
		sf.setDescription( "Snowflake 1" );
		em.persist( sf );

		SnowFlake sf2 = new SnowFlake();
		sf2.setDescription( "Snowflake 2" );
		em.persist( sf2 );

		Cloud cloud = new Cloud();
		cloud.setLength( 23 );
		cloud.getProducedSnowFlakes().add( sf );
		cloud.getProducedSnowFlakes().add( sf2 );

		em.persist( cloud );
		commitOrRollback( true );

		assertNumberOfNodes( 3 );
		assertRelationships( 2 );
	}

	@Test
	public void testUnidirectionalCollectionWithIndex() throws Exception {
		getTransactionManager().begin();
		final EntityManager em = getFactory().createEntityManager();

		Child child1 = new Child();
		child1.setName( "Emmanuel" );
		em.persist( child1 );

		Child child2 = new Child();
		child2.setName( "Christophe" );
		em.persist( child2 );

		Father father = new Father();
		father.getOrderedChildren().add( child1 );
		father.getOrderedChildren().add( child2 );

		em.persist( father );

		Child child3 = new Child();
		child3.setName( "Caroline" );
		em.persist( child3 );

		Child child4 = new Child();
		child4.setName( "Thomas" );
		em.persist( child4 );

		Father father2 = new Father();
		father2.getOrderedChildren().add( child3 );
		father2.getOrderedChildren().add( child4 );

		em.persist( father2 );
		commitOrRollback( true );

		assertNumberOfNodes( 3*2 );
		assertRelationships( 2*2 );
	}

	private void assertRelationships(int rel) throws Exception {
		assertThat( numberOfRelationships() ).as( "Unexpected number of relationships" ).isEqualTo( rel );
	}

	private void assertNumberOfNodes(int nodes) throws Exception {
		assertThat( numberOfNodes() ).as( "Unexpected number of nodes" ).isEqualTo( nodes );
	}

	@After
	public void deleteAll() throws Exception {
		executeQuery( "MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE n, r" );
	}

	private Long numberOfNodes() throws Exception {
		return executeQuery( "MATCH (n) RETURN COUNT(*)" );
	}

	private Long numberOfRelationships() throws Exception {
		return executeQuery( "MATCH (n) - [r] -> () RETURN COUNT(r)" );
	}

	private Long executeQuery(String queryString) throws Exception {
		getTransactionManager().begin();
		final EntityManager em = getFactory().createEntityManager();
		@SuppressWarnings("unchecked")
		List<Object> results = em.createNativeQuery( queryString ).getResultList();
		Long uniqueResult = null;
		if ( !results.isEmpty() ) {
			uniqueResult = (Long) ( (Object[]) results.get( 0 ) )[0];
		}
		commitOrRollback( true );
		em.close();
		if ( uniqueResult == null ) {
			return null;
		}
		return uniqueResult;
	}

	@Test
	public void testCompositeEmbeddedId() throws Exception {
		final NewsID newsOgmID = new NewsID( "How to use Hibernate OGM ?", "Guillaume" );
		final List<Label> newsOgmLabels = labels( "OGM", "hibernate" );
		final News newsOGM = new News( newsOgmID, "Simple, just like ORM but with a NoSQL database", newsOgmLabels );

		boolean operationSuccessful = false;
		getTransactionManager().begin();
		final EntityManager em = getFactory().createEntityManager();

		try {
			em.persist( newsOGM );
			operationSuccessful = true;
		}
		finally {
			commitOrRollback( operationSuccessful );
		}
		em.clear();
		em.close();

		assertNumberOfNodes( 4 );
		assertRelationships( 2 );
	}

	@Test
	public void testEmbeddable() throws Exception {
		getTransactionManager().begin();
		final EntityManager em = getFactory().createEntityManager();

		final Account account = new Account();
		account.setLogin( "emmanuel" );
		account.setPassword( "like I would tell ya" );
		account.setHomeAddress( new Address() );

		final Address address = account.getHomeAddress();
		address.setCity( "Paris" );
		address.setCountry( "France" );
		address.setStreet1( "1 avenue des Champs Elysees" );
		address.setZipCode( "75007" );

		em.persist( account );
		commitOrRollback( true );
		em.close();

		assertNumberOfNodes( 1 );
		assertRelationships( 0 );
	}

	@Test
	public void testElementCollectionOfEmbeddable() throws Exception {
		getTransactionManager().begin();
		EntityManager em = getFactory().createEntityManager();

		final Address address = new Address();
		address.setCity( "Paris" );
		address.setCountry( "France" );
		address.setStreet1( "1 avenue des Champs Elysees" );
		address.setZipCode( "75007" );

		final Address anotherAddress = new Address();
		anotherAddress.setCity( "Rome" );
		anotherAddress.setCountry( "Italy" );
		anotherAddress.setStreet1( "Piazza del Colosseo, 1" );
		anotherAddress.setZipCode( "00184" );

		MultiAddressAccount account = new MultiAddressAccount();
		account.setLogin( "gunnar" );
		account.setPassword( "highly secret" );
		account.getAddresses().add( address );
		account.getAddresses().add( anotherAddress );

		em.persist( account );
		commitOrRollback( true );
		em.close();

		assertNumberOfNodes( 3 );
		assertRelationships( 2 );
	}

	private List<Label> labels(String... names) {
		final List<Label> labels = new ArrayList<Label>();
		for ( String name : names ) {
			labels.add( new Label( name ) );
		}
		return labels;
	}

	@Override
	public Class<?>[] getEntities() {
		return new Class<?>[] {
				AccountOwner.class, BankAccount.class, DistributedRevisionControl.class, JUG.class, Member.class, SalesForce.class,
				SalesGuy.class, Beer.class, Brewery.class, News.class, NewsID.class, Label.class, SnowFlake.class, Cloud.class, Account.class,
				MultiAddressAccount.class,
				Father.class, Child.class
		};
	}
}
