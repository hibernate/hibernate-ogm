/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.storedprocedure.impl;

import java.util.LinkedList;
import java.util.List;

import org.hibernate.ogm.dialect.query.spi.ClosableIterator;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.result.ResultSetOutput;

/**
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
public class NoSQLProcedureOutputImpl implements ResultSetOutput {
	private static final Log log = LoggerFactory.make();
	private Object result;

	public NoSQLProcedureOutputImpl(Object result) {
		this.result = result;
	}

	@Override
	public boolean isResultSet() {
		log.info( "I am called!" );
		return result instanceof ClosableIterator;
	}

	@Override
	public List getResultList() {
		log.info( "I am called!" );
		if ( isResultSet() ) {
			List result = new LinkedList();
			ClosableIterator iterator = (ClosableIterator) result;
			while ( iterator.hasNext() ) {
				result.add( iterator.next() );
			}
			iterator.close();
			return result;
		}

		return null;
	}

	@Override
	public Object getSingleResult() {
		log.info( "I am called!" );
		return result;
	}
}
