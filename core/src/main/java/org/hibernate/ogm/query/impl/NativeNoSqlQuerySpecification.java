/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.query.impl;

import java.util.Collection;

import org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQuerySpecification;

/**
 * Defines a native NoSQL query which may be String-based or object-based.
 *
 * @author Gunnar Morling
 */
public class NativeNoSqlQuerySpecification extends NativeSQLQuerySpecification {

	private final Object queryObject;

	public NativeNoSqlQuerySpecification(String queryString, NativeSQLQueryReturn[] queryReturns, Collection<String> querySpaces) {
		super( queryString, queryReturns, querySpaces );
		this.queryObject = null;
	}

	public NativeNoSqlQuerySpecification(Object queryObject, NativeSQLQueryReturn[] queryReturns, Collection<String> querySpaces) {
		super( queryObject.toString(), queryReturns, querySpaces );
		this.queryObject = queryObject;
	}

	public Object getQueryObject() {
		return queryObject;
	}
}
