/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.utils;

import org.hibernate.HibernateException;

/**
 * A generic exception that can be used when a query should return a single result.
 * <p>
 * It is supposed to be used by the queries that are checking that the mapping is correct.
 *
 * @author Davide D'Alto
 */
public class NotUniqueException extends HibernateException {

	public NotUniqueException() {
		super( "Expected unique result" );
	}
}
