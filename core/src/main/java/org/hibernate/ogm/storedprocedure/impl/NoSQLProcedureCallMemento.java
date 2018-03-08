/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.storedprocedure.impl;

import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.procedure.ProcedureCallMemento;
import org.hibernate.procedure.internal.NoSQLProcedureCallImpl;

public class NoSQLProcedureCallMemento implements ProcedureCallMemento {

	private final ProcedureCallMemento delegate;

	public NoSQLProcedureCallMemento(ProcedureCallMemento delegate) {
		this.delegate = delegate;
	}

	@Override
	public ProcedureCall makeProcedureCall(SharedSessionContractImplementor session) {
		return new NoSQLProcedureCallImpl( session, this );
	}

	@Override
	public Map<String, Object> getHintsMap() {
		return delegate.getHintsMap();
	}

	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> cls) {
		if ( cls.isInstance( delegate ) ) {
			return (T) delegate;
		}
		if ( cls.isInstance( this ) ) {
			return (T) this;
		}
		throw new HibernateException( "Cannot unwrap the following type: " + cls );
	}
}
