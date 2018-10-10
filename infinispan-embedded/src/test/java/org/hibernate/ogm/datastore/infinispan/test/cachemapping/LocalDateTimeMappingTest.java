/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.test.cachemapping;


import static org.hibernate.ogm.datastore.infinispan.utils.InfinispanTestHelper.fetchPropertyMap;
import static org.hibernate.ogm.utils.OgmAssertions.assertThat;

import java.sql.Time;
import java.time.LocalDateTime;
import java.time.Month;

import org.hibernate.ogm.backendtck.type.datetime.LocalDateEntity;
import org.hibernate.ogm.backendtck.type.datetime.LocalTimeEntity;
import org.hibernate.ogm.utils.OgmTestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.infinispan.atomic.FineGrainedAtomicMap;

/**
 * Tests for the mappings of Date types into Infinispan.
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
		FineGrainedAtomicMap<String, Object> propertyMap = fetchPropertyMap( sessionFactory, "LocalDateEntity", "id", dateEntity.getId() );
		assertThat( propertyMap.get( "day" ) ).isEqualTo( dateEntity.getDay() );
	}

	@Test
	public void testLocalDateTimeMapping() {
		FineGrainedAtomicMap<String, Object> propertyMap = fetchPropertyMap( sessionFactory, "LocalDateEntity", "id", dateEntity.getId() );
		assertThat( propertyMap.get( "moment" ) ).isEqualTo( dateEntity.getMoment() );
	}

	@Test
	public void testLocalTimeMapping() {
		FineGrainedAtomicMap<String, Object> propertyMap = fetchPropertyMap( sessionFactory, "LocalTimeEntity", "id", timeEntity.getId() );

		// At the time of writing Hibernate ORM convert a LocalTime field into a java.sql.Time
		// see org.hibernate.type.LocalTimeType, then compare it with *.LocalDateType and *.LocalDateTimeType.
		// In OGM we haven't overridden this behavior.
		Time expected = Time.valueOf( timeEntity.getTime() );

		assertThat( propertyMap.get( "time" ) ).isEqualTo( expected );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { LocalDateEntity.class, LocalTimeEntity.class };
	}
}
