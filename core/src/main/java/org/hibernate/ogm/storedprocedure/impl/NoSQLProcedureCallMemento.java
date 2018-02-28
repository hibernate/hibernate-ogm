/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.storedprocedure.impl;

import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.procedure.ProcedureCallMemento;
import org.hibernate.procedure.internal.NoSQLProcedureCallImpl;

public class NoSQLProcedureCallMemento implements ProcedureCallMemento {

	private final ProcedureCallMemento delegate;

	public NoSQLProcedureCallMemento(ProcedureCallMemento delegate) {
		this.delegate = delegate;
	}

	public ProcedureCall makeProcedureCall(Session session) {
		return makeProcedureCall( (SessionImplementor) session );
	}

	public ProcedureCall makeProcedureCall(SessionImplementor session) {
		return new NoSQLProcedureCallImpl( session, this );
	}

	public Map<String, Object> getHintsMap() {
		return delegate.getHintsMap();
	}

	public <T> T unwrap(Class<T> cls) {
		if ( cls.isInstance( delegate ) ) {
			return (T) delegate;
		}
		if ( cls.isInstance( this ) ) {
			return (T) this;
		}
		throw new HibernateException( "Cannot unwrap the following type: " + cls );
	}

	@Override
	public ProcedureCall makeProcedureCall(SharedSessionContractImplementor session) {
		throw new NotYetImplementedException( "==makeProcedureCall==" );
	}
}
