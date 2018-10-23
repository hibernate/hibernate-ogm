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
public class DuplicatesAndOrderOfAssociationItemsTest extends OgmTestCase {

	public static final String NAME = "Programmers F2F meeting";

	public static final String ADDRESS_0 = "1 avenue des Champs Elysees, Paris, France";
	public static final String ADDRESS_1 = "Piazza del Colosseo, 1, Rome, Italy";
	public static final String ADDRESS_2 = "Charing Cross, London WC2N 5DU, UK";

	public static final String PHONE_0 = "+1-202-555-0333";
	public static final String PHONE_1 = "+1-202-555-0334";
	public static final String PHONE_2 = "+1-202-555-0335";

	public static final String PARTICIPANT_0 = "telegraph4eternity";
	public static final String PARTICIPANT_1 = "fax4ever";
	public static final String PARTICIPANT_2 = "phone4induction";

	public static final String SPEAKER_0 = "unclebob";
	public static final String SPEAKER_1 = "ddalto";

	@After
	public void tearDown() {
		deleteAll( Programmer.class, PARTICIPANT_0, PARTICIPANT_1, PARTICIPANT_2, SPEAKER_0, SPEAKER_1 );
		deleteAll( Meeting.class, NAME );
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1537")
	public void testDuplicatesForElementsCollectionWithoutOrderColumn() {
		Meeting meeting = new Meeting();
		meeting.setName( NAME );
		meeting.addAddress( ADDRESS_0 );
		meeting.addAddress( ADDRESS_0 );
		meeting.addAddress( ADDRESS_1 );
		meeting.addAddress( ADDRESS_0 );
		meeting.addAddress( ADDRESS_2 );

		inTransaction( session -> {
			persistAll( session, meeting );
		} );

		inTransaction( session -> {
			Meeting load = session.load( Meeting.class, NAME );
			assertThat( load.getAddresses() ).hasSize( 3 );
			assertThat( load.getAddresses() )
				.as( "Hibernate OGM does not support duplicates for a collection of embeddebles (issue OGM-1537)."
						+ " If you've solved this issue, congratulations!"
						+ " Please, update the jira and this test accordingly, and thanks a lot" )
				.containsOnly( ADDRESS_0, ADDRESS_1, ADDRESS_2 );
			// it should be:
			// assertThat( load.getAddresses() ).containsExactly( ADDRESS_0, ADDRESS_0, ADDRESS_1, ADDRESS_0, ADDRESS_2 );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1537")
	public void testDuplicatesForOneToManyAssociationWithoutOrderColumn() {
		Programmer participant0 = new Programmer( PARTICIPANT_0 );
		Programmer participant1 = new Programmer( PARTICIPANT_1 );
		Programmer participant2 = new Programmer( PARTICIPANT_2 );

		Meeting meeting = new Meeting();
		meeting.setName( NAME );
		meeting.addParticipant( participant0 );
		meeting.addParticipant( participant1 );
		meeting.addParticipant( participant1 );
		meeting.addParticipant( participant2 );
		meeting.addParticipant( participant1 );

		inTransaction( session -> {
			persistAll( session, meeting, participant0, participant1, participant2 );
		} );

		inTransaction( session -> {
			Meeting load = session.load( Meeting.class, NAME );
			assertThat( load.getParticipants() ).hasSize( 3 );
			assertThat( load.getParticipants() )
				.as( "Hibernate OGM does not support duplicates for associations (issue OGM-1537)."
						+ " If you've solved this issue, congratulations!"
						+ " Please, update the jira and this test accordingly, and thanks a lot" )
				.containsOnly( participant0, participant1, participant2 );

			// It should be:
			// assertThat( load.getParticipants() ).containsExactly( participant0, participant1, participant1, participant2, participant1 );
		} );
	}

	@Test
	public void testOneToManyAssociationWithOrderColumn() {
		Programmer speaker0 = new Programmer( SPEAKER_0 );
		Programmer speaker1 = new Programmer( SPEAKER_1 );

		Meeting meeting = new Meeting();
		meeting.setName( NAME );
		meeting.addSpeaker( speaker0 );
		meeting.addSpeaker( speaker1 );

		inTransaction( session -> {
			persistAll( session, meeting, speaker0, speaker1 );
		} );

		inTransaction( session -> {
			Meeting load = session.load( Meeting.class, NAME );
			assertThat( load.getSpeakers() ).containsExactly( speaker0, speaker1 );
		} );
	}

	@Test
	public void testDuplicatesForOneToManyAssociationWithOrderColumn() {
		Programmer speaker0 = new Programmer( SPEAKER_0 );
		Programmer speaker1 = new Programmer( SPEAKER_1 );

		Meeting meeting = new Meeting();
		meeting.setName( NAME );
		meeting.addSpeaker( speaker0 );
		meeting.addSpeaker( speaker0 );
		meeting.addSpeaker( speaker1 );
		meeting.addSpeaker( speaker0 );

		inTransaction( session -> {
			persistAll( session, meeting, speaker0, speaker1 );
		} );

		inTransaction( session -> {
			Meeting load = session.load( Meeting.class, NAME );
			assertThat( load.getSpeakers() ).containsExactly( speaker0, speaker0, speaker1, speaker0 );
		} );
	}

	@Test
	public void testDuplicatesForElementsCollectionWithOrderColumn() {
		Meeting meeting = new Meeting();
		meeting.setName( NAME );
		meeting.addPhone( PHONE_0 );
		meeting.addPhone( PHONE_1 );
		meeting.addPhone( PHONE_0 );
		meeting.addPhone( PHONE_0 );

		inTransaction( session -> {
			persistAll( session, meeting );
		} );

		inTransaction( session -> {
			Meeting load = session.load( Meeting.class, NAME );
			assertThat( load.getPhones() ).containsExactly( PHONE_0, PHONE_1, PHONE_0, PHONE_0 );
		} );
	}

	@Test
	public void testOrderColumnWithElementsCollection() {
		Meeting meeting = new Meeting();
		meeting.setName( NAME );
		meeting.addPhone( PHONE_2 );
		meeting.addPhone( PHONE_0 );
		meeting.addPhone( PHONE_1 );

		inTransaction( session -> {
			persistAll( session, meeting );
		} );

		inTransaction( session -> {
			Meeting load = session.load( Meeting.class, NAME );
			assertThat( load.getPhones() ).containsExactly( PHONE_2, PHONE_0, PHONE_1 );
		} );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Meeting.class, Programmer.class };
	}
}
