/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm;

import org.hibernate.Session;
import org.hibernate.ogm.query.NoSQLQuery;

/**
 * Session-level functionality specific to Hibernate OGM.
 *
 * @author Gunnar Morling
 */
public interface OgmSession extends Session {

	/**
	 * Creates a native NoSQL query.
	 *
	 * @param nativeQuery A native query, in the format supported by the current data store.
	 * @return A native NoSQL query.
	 */
	NoSQLQuery createNativeQuery(String nativeQuery);

	/**
	 * Use {@link OgmSession#createNativeQuery(String)} instead.
	 */
	@Override
	@Deprecated
	NoSQLQuery createSQLQuery(String queryString);

	@Override
	OgmSessionFactory getSessionFactory();
}
