/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test.query.nativequery;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.ogm.backendtck.queries.pagination.PaginationUseCases;
import org.hibernate.ogm.backendtck.queries.pagination.Poem;

/**
 * Test pagination with Cypher queries.
 *
 * @author Davide D'Alto
 */
public class Neo4jPaginationWithCypherTest extends PaginationUseCases {

	private final String CYPHER_QUERY = "MATCH (p:" + Poem.TABLE_NAME + ") RETURN p ORDER BY p.name";

	@SuppressWarnings("unchecked")
	protected List<Poem> findPoemsSortedAlphabetically(Session session, int startPosition, int maxResult) {
		List<Poem> result = session.createNativeQuery( CYPHER_QUERY )
				.addEntity( Poem.TABLE_NAME, Poem.class )
				.setFirstResult( startPosition )
				.setMaxResults( maxResult )
				.list();
		return result;
	}
}
