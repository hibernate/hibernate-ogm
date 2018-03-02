/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.query.nativequery;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.ogm.backendtck.queries.pagination.PaginationUseCases;
import org.hibernate.ogm.backendtck.queries.pagination.Poem;

/**
 * Test pagination with a native aggregate query in MongoDB.
 *
 * @author Davide D'Alto
 */
public class MongoDBPaginationWithAggregateTest extends PaginationUseCases {

	public static final String NATIVE_QUERY = "db." + Poem.TABLE_NAME + ".aggregate( [ { '$match' : { 'author' : 'Oscar Wilde' } }, { '$sort': {'name': 1 } } ] )";

	@SuppressWarnings("unchecked")
	protected List<Poem> findPoemsSortedAlphabetically(Session session, int startPosition, int maxResult) {
		List<Poem> result = session.createNativeQuery( NATIVE_QUERY )
				.addEntity( Poem.TABLE_NAME, Poem.class )
				.setFirstResult( startPosition )
				.setMaxResults( maxResult )
				.list();
		return result;
	}
}
