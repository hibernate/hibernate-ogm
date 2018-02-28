/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.storedprocedure.impl;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;

import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.result.ResultSetOutput;

/**
 * Models a return that is a result set for NoSQL stored procedure.
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
public class NoSQLProcedureResultSetOutputImpl implements ResultSetOutput {
	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );
	private final List<?> resultList;
	private final boolean isResultRefCursor;

	public NoSQLProcedureResultSetOutputImpl(List<?> resultList, boolean isResultRefCursor) {
		this.resultList = resultList;
		this.isResultRefCursor = isResultRefCursor;
	}

	@Override
	public boolean isResultSet() {
		return isResultRefCursor;
	}

	@Override
	public List getResultList() {
		if ( isResultSet() ) {
			return resultList;
		}
		//need return list of one value
		return Collections.singletonList( getSingleResult() );
	}

	@Override
	public Object getSingleResult() {
		//it is primitive result
		//@todo check for empty.
		Tuple firstTuple = (Tuple) resultList.get( 0 );
		String firstFieldName = firstTuple.getColumnNames().iterator().next();
		return  firstTuple.get( firstFieldName );
	}
}
