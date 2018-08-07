/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.type.datetime;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.utils.GridDialectType.MONGODB;
import static org.hibernate.ogm.utils.GridDialectType.NEO4J_REMOTE;

import java.time.LocalDateTime;
import java.time.Month;

import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.SkipByGridDialect;
import org.hibernate.ogm.utils.TestForIssue;

import org.junit.After;
import org.junit.Test;

/**
 * Tests support for
 * {@link java.time.LocalDate} and {@link java.time.LocalDateTime}
 * as property types.
 *
 * @author Fabio Massimo Ercoli
 */
@TestForIssue(jiraKey = "OGM-1515")
@SkipByGridDialect(value = { MONGODB, NEO4J_REMOTE }, comment = "Dialects need to handle java.time.LocalDate and java.time.LocalDateTime as property types")
public class LocalDateTest extends OgmTestCase  {

	private LocalDateEntity entity;

	@Test
	public void testLocalDatePropertyValues() {
		LocalDateTime originalMoment = LocalDateTime.of( 2018, Month.AUGUST, 7, 12, 41, 50, 0 );
		entity = new LocalDateEntity( 2, "A entity using Java8 local date", originalMoment );

		inTransaction( session -> session.persist( entity ) );

		inTransaction( session -> {
			LocalDateEntity reloaded = session.load( LocalDateEntity.class, 2 );

			assertThat( reloaded.getMoment() ).isEqualTo( originalMoment );
			assertThat( reloaded.getDay() ).isEqualTo( originalMoment.toLocalDate() );
		} );
	}

	@After
	public void cleanUp() {
		inTransaction( session -> session.delete( entity ) );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {LocalDateEntity.class};
	}
}
