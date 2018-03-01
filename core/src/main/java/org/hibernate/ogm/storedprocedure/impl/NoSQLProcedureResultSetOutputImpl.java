/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.storedprocedure.impl;

import java.util.List;
import java.util.function.Supplier;

import org.hibernate.result.ResultSetOutput;

/**
 * Implementation of {@link ResultSetOutput} for OGM.
 * <p>
 * This is a copy of the implementation in ORM that is package private and therefore we cannot use.
 *
 * @see org.hibernate.result.internal.ResultSetOutputImpl
 * @author Davide D'Alto
 */
class NoSQLProcedureResultSetOutputImpl implements ResultSetOutput {

	private final Supplier<List<?>> resultSetSupplier;

	public NoSQLProcedureResultSetOutputImpl(List<?> results) {
		this.resultSetSupplier = () -> results;
	}

	public NoSQLProcedureResultSetOutputImpl(Supplier<List<?>> resultSetSupplier) {
		this.resultSetSupplier = resultSetSupplier;
	}

	@Override
	public boolean isResultSet() {
		return true;
	}

	@Override
	public List<?> getResultList() {
		return resultSetSupplier.get();
	}

	@Override
	public Object getSingleResult() {
		final List<?> results = getResultList();
		if ( results == null || results.isEmpty() ) {
			return null;
		}
		else {
			return results.get( 0 );
		}
	}
}
