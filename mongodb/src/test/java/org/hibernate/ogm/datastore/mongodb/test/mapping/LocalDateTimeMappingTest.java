/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.mapping;

import static org.hibernate.ogm.datastore.mongodb.utils.MongoDBTestHelper.assertDocument;

import java.time.LocalDateTime;
import java.time.Month;

import org.hibernate.ogm.backendtck.type.datetime.LocalDateEntity;
import org.hibernate.ogm.backendtck.type.datetime.LocalTimeEntity;
import org.hibernate.ogm.utils.OgmTestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for the mappings of Date types into MongoDB.
 *
 * @author Fabio Massimo Ercoli
 */
public class LocalDateTimeMappingTest extends OgmTestCase {

	private LocalDateEntity dateEntity;
	private LocalTimeEntity timeEntity;

	@Before
	public void persistEntities() {
		LocalDateTime moment = LocalDateTime.of( 2018, Month.AUGUST, 7, 12, 41, 50, 0 );
		dateEntity = new LocalDateEntity( 7, "A entity using Java8 local date", moment );
		timeEntity = new LocalTimeEntity( 3, "A entity using Java8 local time", moment.toLocalTime() );

		inTransaction( session -> {
			session.persist( dateEntity );
			session.persist( timeEntity );
		} );
	}

	@After
	public void deleteEntities() {
		inTransaction( session -> {
			session.delete( dateEntity );
			session.delete( timeEntity );
		} );
	}

	@Test
	public void testLocalDateMapping() {
		assertDocument(
				sessionFactory,
				"LocalDateEntity",
				"{ '_id' : " + dateEntity.getId() + " }",
				"{ 'day' : 1 }",
				"{ " +
						"'_id' : " + dateEntity.getId() + ", " +
						"'day' : '2018-08-07'" +
						"}"
		);
	}

	@Test
	public void testLocalDateTimeMapping() {
		assertDocument(
				sessionFactory,
				"LocalDateEntity",
				"{ '_id' : " + dateEntity.getId() + " }",
				"{ 'moment' : 1 }",
				"{ " +
						"'_id' : " + dateEntity.getId() + ", " +
						"'moment' : '2018-08-07 12:41:50.0'" +
						"}"
		);
	}

	@Test
	public void testLocalTimeMapping() {
		assertDocument(
				sessionFactory,
				"LocalTimeEntity",
				"{ '_id' : " + timeEntity.getId() + " }",
				"{ 'time' : 1 }",
				"{ " +
						"'_id' : " + timeEntity.getId() + ", " +
						"'time' : '12:41:50'" +
						"}"
		);
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { LocalDateEntity.class, LocalTimeEntity.class };
	}
}
