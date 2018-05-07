/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.test.query.nativequery;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.ogm.backendtck.queries.pagination.PaginationUseCases;
import org.hibernate.ogm.backendtck.queries.pagination.Poem;
import org.hibernate.ogm.datastore.infinispanremote.utils.InfinispanRemoteServerRunner;

import org.junit.runner.RunWith;

/**
 * Test pagination on Infinispan Server native query
 *
 * @author Fabio Massimo Ercoli
 */
@RunWith(InfinispanRemoteServerRunner.class)
public class InfinispanRemotePaginationTest extends PaginationUseCases {

	public static final String NATIVE_QUERY = "from HibernateOGMGenerated.POEM where author = 'Oscar Wilde' order by name";

	@Override
	protected List<Poem> findPoemsSortedAlphabetically(Session session, int startPosition, int maxResult) {
		List<Poem> result = session.createNativeQuery( NATIVE_QUERY )
				.addEntity( Poem.TABLE_NAME, Poem.class )
				.setFirstResult( startPosition )
				.setMaxResults( maxResult )
				.list();
		return result;
	}
}
