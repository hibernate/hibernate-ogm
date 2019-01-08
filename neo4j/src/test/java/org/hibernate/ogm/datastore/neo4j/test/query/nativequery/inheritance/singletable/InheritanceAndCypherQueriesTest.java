/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test.query.nativequery.inheritance.singletable;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.datastore.neo4j.test.query.nativequery.inheritance.singletable.Dinosauria.ORNITHISCHIA_DISC;

import java.util.List;

import org.hibernate.ogm.utils.TestForIssue;
import org.hibernate.ogm.utils.jpa.OgmJpaTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@TestForIssue(jiraKey = "OGM-1552")
public class InheritanceAndCypherQueriesTest extends OgmJpaTestCase {

	private static final String TREX = "Tyrannosaurus Rex";
	private static final String STEG = "Stegosaurus";

	@Before
	public void populateDB() {
		inTransaction( em -> {
			Ornithischia stegosaurus = new Ornithischia( STEG );
			stegosaurus.setHerbivore( true );
			em.persist( stegosaurus );

			Saurischia trex = new Saurischia( TREX );
			trex.setCarnivore( true );
			em.persist( trex );
		} );
	}

	@After
	public void deleteAll() throws Exception {
		removeEntities();
	}

	@Test
	public void testCypherReturnsSubType() {
		inTransaction( em -> {
			List<Ornithischia> result = em.createNativeQuery( "MATCH (s:Dinosauria) WHERE s.type = " + ORNITHISCHIA_DISC + " RETURN s", Ornithischia.class )
					.getResultList();

			assertThat( result ).onProperty( "name" ).containsExactly( STEG );
		} );
	}

	@Test
	public void testCypherReturnsSubtypeAsSuperType() {
		inTransaction( em -> {
			List<Dinosauria> result = em.createNativeQuery( "MATCH (s:Dinosauria) WHERE s.type = " + ORNITHISCHIA_DISC + " RETURN s", Dinosauria.class )
					.getResultList();

			assertThat( result ).onProperty( "name" ).containsExactly( STEG );
		} );
	}

	@Test
	public void testCypherReturnsAllValuesAsSuperType() {
		inTransaction( em -> {
			List<Dinosauria> result = em.createNativeQuery( "MATCH (d:Dinosauria) RETURN d", Dinosauria.class )
					.getResultList();

			assertThat( result ).onProperty( "name" ).containsOnly( STEG, TREX );
			assertThat( result ).hasSize( 2 );
		} );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ Dinosauria.class, Ornithischia.class, Saurischia.class };
	}
}
