/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.integration.wildfly.testcase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.GregorianCalendar;
import java.util.List;

import javax.inject.Inject;

import org.hibernate.ogm.test.integration.wildfly.testcase.controller.MagicCardsCollectionBean;
import org.hibernate.ogm.test.integration.wildfly.testcase.model.MagicCard;
import org.junit.Test;

/**
 * Base class containing some tests which use Hibernate Search too.
 * Needs to be extended to become a concrete Arquillian test and define
 * deployment descriptors.
 */
public abstract class MagiccardsDatabaseScenario {

	@Inject
	MagicCardsCollectionBean cardsCollection;

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
