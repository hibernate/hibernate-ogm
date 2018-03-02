/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.queries.pagination;

import java.util.List;

import org.hibernate.Session;

/**
 * Test pagination with a JPQL query compatible with all dialects.
 *
 * @author Davide D'Alto
 */
public class PaginationWithJPQLTest extends PaginationUseCases {

	public static final String JPQL_QUERY = "SELECT p FROM Poem p WHERE p.author = 'Oscar Wilde' ORDER BY p.name";

	protected List<Poem> findPoemsSortedAlphabetically(Session session, int startPosition, int maxResult) {
		List<Poem> result = session.createQuery( JPQL_QUERY, Poem.class )
				.setFirstResult( startPosition )
				.setMaxResults( maxResult )
				.list();
		return result;
	}
}
