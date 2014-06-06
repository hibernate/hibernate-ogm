/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
import org.hibernate.ogm.datastore.neo4j.dialect.impl.Neo4jSequenceGenerator;
import org.hibernate.ogm.utils.jpa.JpaTestCase;
import org.junit.After;
import org.junit.Test;

/**
 * These tests check that the expected number of nodes and relationship is created on the db. They also checks for the
 * existence of the expected attributes and labels.
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

		assertExpectedMapping( "(n:JUG:ENTITY {jug_id: '" + jug.getId() + "', name: '" + jug.getName() + "' })" );
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

		assertExpectedMapping( "(:DistributedRevisionControl:ENTITY {id: " + git.getId() + ", name: '" + git.getName() + "' })" );
		assertExpectedMapping( "(:DistributedRevisionControl:hibernate_sequences {" + Neo4jSequenceGenerator.SEQUENCE_NAME_PROPERTY
				+ ": 'DistributedRevisionControl', DistributedRevisionControl: 2 })" );
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

		String ownerNode = "(owner:AccountOwner:ENTITY {id: '" + owner.getId() + "', SSN: '" + owner.getSSN() + "' })";
		String barcklaysNode = "(barclays:BankAccount:ENTITY {id: '" + barclays.getId() + "', accountNumber: '" + barclays.getAccountNumber() + "' })";
		String sogeNode = "(soge:BankAccount:ENTITY {id: '" + soge.getId() + "', accountNumber: '" + soge.getAccountNumber() + "' })";

		// Bidirectional relationship with Barcklays
		assertExpectedMapping( ownerNode + "- [:bankAccounts] -> " + barcklaysNode );
		assertExpectedMapping( barcklaysNode + " - [:owners] -> " + ownerNode );

		// Bidirectional relationship with Sorge
		assertExpectedMapping( ownerNode + " - [:bankAccounts] -> " + sogeNode );
		assertExpectedMapping( sogeNode + " - [:owners] -> " + ownerNode );
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

		assertExpectedMapping( "(n:JUG:ENTITY {jug_id: '" + jug.getId() + "', name: '" + jug.getName() + "' })" );
		assertExpectedMapping( "(n:Member:ENTITY {member_id: '" + emmanuel.getId() + "', name: '" + emmanuel.getName() + "', memberOf_jug_id: '" + jug.getId() + "' })" );
		assertExpectedMapping( "(n:Member:ENTITY {member_id: '" + jerome.getId() + "' , name: '" + jerome.getName() + "', memberOf_jug_id: '" + jug.getId() + "' })" );
	}

	@Test
	public void testUnidirectionalCollection() throws Exception {
		getTransactionManager().begin();
		final EntityManager em = getFactory().createEntityManager();

		SnowFlake sf1 = new SnowFlake();
		sf1.setDescription( "Snowflake 1" );
		em.persist( sf1 );

		SnowFlake sf2 = new SnowFlake();
		sf2.setDescription( "Snowflake 2" );
		em.persist( sf2 );

		Cloud cloud = new Cloud();
		cloud.setLength( 23 );
		cloud.getProducedSnowFlakes().add( sf1 );
		cloud.getProducedSnowFlakes().add( sf2 );

		em.persist( cloud );
		commitOrRollback( true );

		assertNumberOfNodes( 3 );
		assertRelationships( 2 );

		String cloudNode = "(:Cloud:ENTITY {id: '" + cloud.getId() + "', length: 23 })";
		String sf1Node = "(sf1:SnowFlake:ENTITY {id: '" + sf1.getId() + "', description: '" + sf1.getDescription() + "' })";
		String sf2Node = "(sf2:SnowFlake:ENTITY {id: '" + sf2.getId() + "', description: '" + sf2.getDescription() + "' })";

		assertExpectedMapping( cloudNode + " - [:producedSnowFlakes] -> " + sf1Node );
		assertExpectedMapping( cloudNode + " - [:producedSnowFlakes] -> " + sf2Node );
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

		Father father1 = new Father();
		father1.getOrderedChildren().add( child1 );
		father1.getOrderedChildren().add( child2 );

		em.persist( father1 );

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

		assertNumberOfNodes( 6 );
		assertRelationships( 4 );

		String father1Node = "(:Father:ENTITY {id: '" + father1.getId() + "' })";
		String child1Node = "(:Child:ENTITY {id: '" + child1.getId() + "', name: '" + child1.getName() + "' })";
		String child2Node = "(:Child:ENTITY {id: '" + child2.getId() + "', name: '" + child2.getName() + "' })";

		assertExpectedMapping( father1Node + " - [:orderedChildren {birthorder: 0}] -> " + child1Node );
		assertExpectedMapping( father1Node + " - [:orderedChildren {birthorder: 1}] -> " + child2Node );

		String father2Node = "(:Father:ENTITY {id: '" + father2.getId() + "' })";
		String child3Node = "(:Child:ENTITY {id: '" + child3.getId() + "', name: '" + child3.getName() + "' })";
		String child4Node = "(:Child:ENTITY {id: '" + child4.getId() + "', name: '" + child4.getName() + "' })";

		assertExpectedMapping( father2Node + " - [:orderedChildren {birthorder: 0}] -> " + child3Node );
		assertExpectedMapping( father2Node + " - [:orderedChildren {birthorder: 1}] -> " + child4Node );
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

		assertExpectedMapping( "(n:Account:ENTITY {"
				+ "  `login`: '" + account.getLogin() + "'"
				+ ", `password`: '" + account.getPassword() + "'"
				+ ", `homeAddress.street1`: '" + account.getHomeAddress().getStreet1() + "'"
				+ ", `homeAddress.city`: '" + account.getHomeAddress().getCity() + "'"
				+ ", `homeAddress.country`: '" + account.getHomeAddress().getCountry() + "'"
				+ ", `postal_code`: '" + account.getHomeAddress().getZipCode() + "'"
				+ " })" );
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

		String accountNode = "(:MultiAddressAccount:ENTITY {"
				+ "  `login`: '" + account.getLogin() + "'"
				+ ", `password`: '" + account.getPassword() + "'"
				+ " })";

		String addressNode = "(:MultiAddressAccount_addresses:EMBEDDED {"
				+ "  street1: '" + address.getStreet1() + "'"
				+ ", city: '" + address.getCity() + "'"
				+ ", country: '" + address.getCountry() + "'"
				+ ", postal_code: '" + address.getZipCode() + "'"
				+ " })";

		String anotherAddressNode = "(:MultiAddressAccount_addresses:EMBEDDED {"
				+ "  street1: '" + anotherAddress.getStreet1() + "'"
				+ ", city: '" + anotherAddress.getCity() + "'"
				+ ", country: '" + anotherAddress.getCountry() + "'"
				+ ", postal_code: '" + anotherAddress.getZipCode() + "'"
				+ " })";

		assertExpectedMapping( accountNode + " - [:addresses] -> " + addressNode );
		assertExpectedMapping( accountNode + " - [:addresses] -> " + anotherAddressNode );
	}

	@Test
	public void testBidirectionalManyToOneRegular() throws Exception {
		getTransactionManager().begin();
		EntityManager em = getFactory().createEntityManager();

		SalesForce force = new SalesForce();
		force.setCorporation( "Red Hat" );
		em.persist( force );

		SalesGuy eric = new SalesGuy();
		eric.setName( "Eric" );
		eric.setSalesForce( force );
		force.getSalesGuys().add( eric );
		em.persist( eric );

		SalesGuy simon = new SalesGuy();
		simon.setName( "Simon" );
		simon.setSalesForce( force );
		force.getSalesGuys().add( simon );
		em.persist( simon );

		commitOrRollback( true );
		em.close();

		assertNumberOfNodes( 3 );
		assertRelationships( 2 );

		String forceNode = "(:SalesForce:ENTITY {"
				+ "  id: '" + force.getId() + "'"
				+ ", corporation: '" + force.getCorporation() + "'"
				+ " })";

		String ericNode = "(:SalesGuy:ENTITY {"
				+ "  id: '" + eric.getId() + "'"
				+ ", name: '" + eric.getName() + "'"
				+ ", salesForce_id: '" + force.getId() + "'"
				+ " })";

		String simonNode = "(:SalesGuy:ENTITY {"
				+ "  id: '" + simon.getId() + "'"
				+ ", name: '" + simon.getName() + "'"
				+ ", salesForce_id: '" + force.getId() + "'"
				+ " })";

		assertExpectedMapping( forceNode + " - [:salesGuys] -> " + ericNode );
		assertExpectedMapping( forceNode + " - [:salesGuys] -> " + simonNode );
	}

	private void assertRelationships(int rel) throws Exception {
		assertThat( numberOfRelationships() ).as( "Unexpected number of relationships" ).isEqualTo( rel );
	}

	private void assertNumberOfNodes(int nodes) throws Exception {
		assertThat( numberOfNodes() ).as( "Unexpected number of nodes" ).isEqualTo( nodes );
	}

	private void assertExpectedMapping(String element) throws Exception {
		assertThat( executeQuery( "MATCH " + element + " RETURN 1" ) ).as( "Not found in the db: " + element ).isNotNull();
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
