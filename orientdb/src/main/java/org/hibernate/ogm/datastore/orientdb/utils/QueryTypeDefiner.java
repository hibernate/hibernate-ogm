/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.orientdb.utils;

/**
 * Utility class for define type of query (insert or update)
 *
 * @author Sergey Chernolyas &lt;sergey.chernolyas@gmail.com&gt;
 */

public class QueryTypeDefiner {

	/**
	 * Enumeration with query types
	 */
	public static enum QueryType {
		INSERT, UPDATE, ERROR;
	}

	/**
	 * define type of query
	 *
	 * @param existsInDB is primary key found in database?
	 * @param isNewSnapshot is new snapshot?
	 * @return type
	 */
	public static QueryType define(boolean existsInDB, boolean isNewSnapshot) {
		QueryType type = QueryType.ERROR;

		if ( isNewSnapshot && !existsInDB ) {
			type = QueryType.INSERT;
		}
		else if ( existsInDB ) {
			type = QueryType.UPDATE;
		}
		return type;
	}

}
