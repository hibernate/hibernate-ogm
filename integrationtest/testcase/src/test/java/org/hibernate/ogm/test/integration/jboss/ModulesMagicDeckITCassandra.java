/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.integration.jboss;

import static org.hibernate.ogm.test.integration.jboss.util.CassandraConfigurationHelper.setCassandraHostName;
import static org.hibernate.ogm.test.integration.jboss.util.CassandraConfigurationHelper.setCassandraPort;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.GregorianCalendar;
import java.util.List;

import javax.inject.Inject;

import org.hibernate.ogm.test.integration.jboss.controller.MagicCardsCollectionBean;
import org.hibernate.ogm.test.integration.jboss.model.MagicCard;
import org.hibernate.ogm.test.integration.jboss.util.ModulesHelper;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceDescriptor;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceUnit;
import org.jboss.shrinkwrap.descriptor.api.persistence20.Properties;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test for the Hibernate OGM module in WildFly using Cassandra.
 * The class name has to finish with "ITCassandra" for the test to be enabled in the right profile.
 * <p>
 * At time of writing, the Cassandra GridDialect implementation is not ready to handle associations
 * so we'll use an over simplified model.
 *
 * @author Sanne Grinovero
 */
@RunWith(Arquillian.class)
public class ModulesMagicDeckITCassandra {

	@Inject
	MagicCardsCollectionBean cardsCollection;

	@Deployment
	public static Archive<?> createTestArchive() {
		WebArchive webArchive = ShrinkWrap
				.create( WebArchive.class, "modules-magic-cassandra.war" )
				.addClasses( MagicCard.class, MagicCardsCollectionBean.class );
		String persistenceXml = persistenceXml().exportAsString();
		webArchive.addAsResource( new StringAsset( persistenceXml ), "META-INF/persistence.xml" );
		ModulesHelper.addModulesDependencyDeclaration( webArchive, "org.hibernate:ogm services, org.hibernate.ogm.cassandra services, org.hibernate.search.orm:${hibernate-search.module.slot} services" );
		return webArchive;
	}

	private static PersistenceDescriptor persistenceXml() {
		PersistenceDescriptor descriptor = Descriptors.create( PersistenceDescriptor.class );
		Properties<PersistenceUnit<PersistenceDescriptor>> properties = descriptor
			.version( "2.0" )
			.createPersistenceUnit()
				.name( "primary" )
				.provider( "org.hibernate.ogm.jpa.HibernateOgmPersistence" )
				.clazz( MagicCard.class.getName() )
				.getOrCreateProperties()
					.createProperty().name( "hibernate.search.default.directory_provider" ).value( "ram" ).up()
					.createProperty().name( "hibernate.ogm.datastore.database" ).value( "ogm_test_database" ).up()
					.createProperty().name( "hibernate.ogm.datastore.provider" ).value( "cassandra_experimental" ).up();

		setCassandraHostName( properties );
		setCassandraPort( properties );

		return descriptor;
	}

	@Test
	public void shouldGenerateAnId() throws Exception {
		MagicCard card = makeADragon();
		cardsCollection.storeCard( card );
		assertNotNull( card.getId() );
	}

	@Test
	public void shouldFindCardById() throws Exception {
		MagicCard card = makeADragon();
		cardsCollection.storeCard( card );
		assertNotNull( card.getId() );

		MagicCard loaded = cardsCollection.loadById( card.getId() );
		assertEquals( card.getName(), loaded.getName() );
		assertEquals( Integer.valueOf( 5 ), loaded.getPower() );
		assertEquals( "4RR", loaded.getManacost() );
	}

	@Test
	public void shouldFindCardByName() throws Exception {
		cardsCollection.storeCard( makeADragon() );
		cardsCollection.storeCard( makeADragonWelp() );

		List<MagicCard> results = cardsCollection.findByName( "Shivan Dragon" );
		assertEquals( 1, results.size() );
		MagicCard loaded = results.get( 0 );
		assertEquals( "Shivan Dragon", loaded.getName() );
		assertEquals( Integer.valueOf( 5 ), loaded.getPower() );
		assertEquals( "4RR", loaded.getManacost() );
	}

	private MagicCard makeADragon() {
		MagicCard shivan = new MagicCard();
		shivan.setName( "Shivan Dragon" );
		shivan.setArtist( "Melissa Benson" );
		shivan.setManacost( "4RR" );
		shivan.setPower( 5 );
		shivan.setThoughness( 5 );
		shivan.setPublicationDate( new GregorianCalendar( 1993, 8, 5 ).getTime() );
		return shivan;
	}

	private MagicCard makeADragonWelp() {
		MagicCard welp = new MagicCard();
		welp.setName( "Dragon Welp" );
		welp.setArtist( "Amy Weber" );
		welp.setManacost( "2RR" );
		welp.setPower( 2 );
		welp.setThoughness( 3 );
		welp.setPublicationDate( new GregorianCalendar( 1993, 8, 5 ).getTime() );
		return welp;
	}

}
