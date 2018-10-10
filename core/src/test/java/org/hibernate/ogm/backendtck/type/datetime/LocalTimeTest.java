/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.type.datetime;

import static org.fest.assertions.Assertions.assertThat;

import java.time.LocalTime;

import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.TestForIssue;

import org.junit.After;
import org.junit.Test;

/**
 * Tests support for
 * {@link java.time.LocalTime}
 * as property type.
 *
 * @author Fabio Massimo Ercoli
 */
@TestForIssue(jiraKey = "OGM-1515")
public class LocalTimeTest extends OgmTestCase {

	private LocalTimeEntity entity;

	@Test
	public void testLocalTimePropertyValue() {
		LocalTime originalTime = LocalTime.of( 12, 49, 59, 0 );
		entity = new LocalTimeEntity( 1, "A entity using Java8 local time", originalTime );

		inTransaction( session -> session.persist( entity ) );

		inTransaction( session -> {
			LocalTimeEntity reloaded = session.load( LocalTimeEntity.class, 1 );

			assertThat( reloaded.getTime() ).isEqualTo( originalTime );
		} );
	}

	@After
	public void cleanUp() {
		inTransaction( session -> session.delete( entity ) );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { LocalTimeEntity.class };
	}
}
