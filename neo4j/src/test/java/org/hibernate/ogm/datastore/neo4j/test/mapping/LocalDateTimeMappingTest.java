/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test.mapping;

import static org.hibernate.ogm.datastore.neo4j.dialect.impl.NodeLabel.ENTITY;
import static org.hibernate.ogm.datastore.neo4j.test.dsl.GraphAssertions.node;

import java.time.LocalDateTime;
import java.time.Month;

import org.hibernate.ogm.backendtck.type.datetime.LocalDateEntity;
import org.hibernate.ogm.backendtck.type.datetime.LocalTimeEntity;
import org.hibernate.ogm.datastore.neo4j.test.dsl.NodeForGraphAssertions;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for the mappings of Date types into Neo4j.
 *
 * @author Fabio Massimo Ercoli
 */
public class LocalDateTimeMappingTest extends Neo4jJpaTestCase {

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
			session.refresh( dateEntity );
			session.remove( dateEntity );
			session.refresh( timeEntity );
			session.remove( timeEntity );
		} );
	}

	@Test
	public void testLocalDateMapping() throws Exception {
		NodeForGraphAssertions dateEntityNode = node( "dateEntity", LocalDateEntity.class.getSimpleName(), ENTITY.name() )
				.property( "id", dateEntity.getId() )
				.property( "name", "A entity using Java8 local date" )
				.property( "day", "2018-08-07" )
				.property( "moment", "2018-08-07 12:41:50.0" );

		NodeForGraphAssertions timeEntityNode = node( "timeEntity", LocalTimeEntity.class.getSimpleName(), ENTITY.name() )
				.property( "id", timeEntity.getId() )
				.property( "name", "A entity using Java8 local time" )
				.property( "time", "12:41:50" );

		assertThatOnlyTheseNodesExist( dateEntityNode, timeEntityNode );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { LocalDateEntity.class, LocalTimeEntity.class };
	}
}
