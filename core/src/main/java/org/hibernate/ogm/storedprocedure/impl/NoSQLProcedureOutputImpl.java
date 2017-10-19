/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.storedprocedure.impl;

import java.util.LinkedList;
import java.util.List;

import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.result.ResultSetOutput;

/**
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
public class NoSQLProcedureOutputImpl implements ResultSetOutput {
	private static final Log log = LoggerFactory.make();
	private List<?> resultList = new LinkedList();

	public NoSQLProcedureOutputImpl(List<?> resultList) {
		this.resultList = resultList;
	}

	@Override
	public boolean isResultSet() {

		return !resultList.isEmpty() && !isTuple();
	}

	private boolean isTuple() {
		return resultList.get( 0 ) instanceof Tuple;
	}

	@Override
	public List getResultList() {
		log.info( "I am called!" );
		if ( isResultSet() ) {

			return resultList;
		}

		return null;
	}

	@Override
	public Object getSingleResult() {
		log.info( "I am called!" );
		//@todo check ... it can be not a Tuple
		//it is primitive result
		Tuple firstTuple = (Tuple) resultList.get( 0 );
		String firstFieldName = firstTuple.getColumnNames().iterator().next();
		return  firstTuple.get( firstFieldName );
	}
}
