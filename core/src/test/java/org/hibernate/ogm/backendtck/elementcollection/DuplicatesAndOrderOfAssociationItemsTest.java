/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.elementcollection;

import static org.fest.assertions.Assertions.assertThat;

import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.TestForIssue;

import org.junit.After;
import org.junit.Test;

/**
 * Tests for associations and collections of elements containing duplicates, with or without {@link javax.persistence.OrderColumn}.
 *
 * @author Fabio Massimo Ercoli
 */
@TestForIssue( jiraKey = "OGM-1237" )
public class DuplicatesAndOrderOfAssociationItemsTest extends OgmTestCase {

	public static final String NAME = "Programmers F2F meeting";
	public static final String ADDRESS = "1 avenue des Champs Elysees, Paris, France";
	public static final String ADDRESS_ALT1 = "Piazza del Colosseo, 1, Rome, Italy";
	public static final String ADDRESS_ALT2 = "Charing Cross, London WC2N 5DU, UK";
	public static final String PHONE = "+1-202-555-0333";
	public static final String PHONE_ALT1 = "+1-202-555-0333";
	public static final String PHONE_ALT2 = "+1-202-555-0333";
	public static final String PARTICIPANT_0 = "telegraph4eternity";
	public static final String PARTICIPANT_1 = "fax4ever";
	public static final String PARTICIPANT_2 = "phone4induction";
	public static final String SPEAKER = "unclebob";

	@After
	public void tearDown() {
		deleteAll( Programmer.class, PARTICIPANT_0, PARTICIPANT_1, PARTICIPANT_2, SPEAKER );
		deleteAll( Meeting.class, NAME );
	}

	@Test
	public void testKeepDuplicatesOnAssociationItems() {
		Meeting meeting = new Meeting();
		meeting.setName( NAME );

		meeting.addAddress( ADDRESS );
		meeting.addAddress( ADDRESS_ALT1 );
		meeting.addAddress( ADDRESS_ALT2 );

		meeting.addPhone( PHONE );
		meeting.addPhone( PHONE );
		meeting.addPhone( PHONE );

		Programmer participant0 = new Programmer( PARTICIPANT_0 );
		Programmer participant1 = new Programmer( PARTICIPANT_1 );
		Programmer participant2 = new Programmer( PARTICIPANT_2 );

		meeting.addParticipant( participant0 );
		meeting.addParticipant( participant1 );
		meeting.addParticipant( participant2 );

		Programmer speaker = new Programmer( SPEAKER );
		meeting.addSpeaker( speaker );
		meeting.addSpeaker( speaker );
		meeting.addSpeaker( speaker );

		inTransaction( session -> {
			session.save( participant0 );
			session.save( participant1 );
			session.save( participant2 );
			session.save( speaker );
			session.save( meeting );
		} );

		inTransaction( session -> {
			Meeting load = session.load( Meeting.class, NAME );

			// an embedded list with a defined @OrderColumn keeps duplicates
			assertThat( load.getPhones() ).containsExactly( PHONE, PHONE, PHONE );

			// without an @OrderColumn it may or may not keep duplicates
			assertThat( load.getAddresses() ).containsOnly( ADDRESS, ADDRESS_ALT1, ADDRESS_ALT2 );

			// a join table list with a defined @OrderColumn keeps duplicates
			assertThat( load.getSpeakers() ).containsExactly( speaker, speaker, speaker );

			// without an @OrderColumn it may or may not not keep duplicates
			assertThat( load.getParticipants() ).containsOnly( participant0, participant1, participant2 );
		} );
	}

	@Test
	public void testPreserveOrderOnAssociationItems() {
		Meeting meeting = new Meeting();
		meeting.setName( NAME );

		meeting.addAddress( ADDRESS );
		meeting.addAddress( ADDRESS_ALT1 );
		meeting.addAddress( ADDRESS_ALT2 );

		meeting.addPhone( PHONE );
		meeting.addPhone( PHONE_ALT1 );
		meeting.addPhone( PHONE_ALT2 );

		Programmer participant0 = new Programmer( PARTICIPANT_0 );
		Programmer participant1 = new Programmer( PARTICIPANT_1 );
		Programmer participant2 = new Programmer( PARTICIPANT_2 );

		meeting.addParticipant( participant0 );
		meeting.addParticipant( participant1 );
		meeting.addParticipant( participant2 );

		meeting.addSpeaker( participant0 );
		meeting.addSpeaker( participant1 );
		meeting.addSpeaker( participant2 );

		inTransaction( session -> {
			session.save( participant0 );
			session.save( participant1 );
			session.save( participant2 );
			session.save( meeting );
		} );

		inTransaction( session -> {
			Meeting load = session.load( Meeting.class, NAME );

			// an embedded list with a defined @OrderColumn guarantees the order
			assertThat( load.getPhones() ).containsExactly( PHONE, PHONE_ALT1, PHONE_ALT2 );

			// without an @OrderColumn it doesn't guarantee the order
			assertThat( load.getAddresses() ).containsOnly( ADDRESS, ADDRESS_ALT1, ADDRESS_ALT2 );

			// a join table list with a defined @OrderColumn guarantees the order
			assertThat( load.getSpeakers() ).containsExactly( participant0, participant1, participant2 );

			// without an @OrderColumn it doesn't guarantee the order
			assertThat( load.getParticipants() ).containsOnly( participant0, participant1, participant2 );
		} );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Meeting.class, Programmer.class };
	}
}
