/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.procedure.internal;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.ogm.storedprocedure.impl.NoSQLProcedureCallMemento;
import org.hibernate.ogm.storedprocedure.impl.NoSQLProcedureOutputsImpl;
import org.hibernate.procedure.ProcedureOutputs;

/**
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
public class NoSQLProcedureCallImpl extends ProcedureCallImpl {

	private final NoSQLProcedureCallMemento memento;

	public NoSQLProcedureCallImpl(SharedSessionContractImplementor session, String procedureName) {
		super( session, procedureName );
		this.memento = null;
	}

	public NoSQLProcedureCallImpl(SharedSessionContractImplementor session, String procedureName, Class<?>... resultClasses) {
		super( session, procedureName, resultClasses );
		this.memento = null;
	}

	public NoSQLProcedureCallImpl(SharedSessionContractImplementor session, String procedureName, String... resultSetMappings) {
		super( session, procedureName, resultSetMappings );
		this.memento = null;
	}

	public NoSQLProcedureCallImpl(SharedSessionContractImplementor session, NoSQLProcedureCallMemento memento) {
		super( session, memento.unwrap( ProcedureCallMementoImpl.class ) );
		this.memento = memento;
	}

	@Override
	public ProcedureOutputs getOutputs() {
		return new NoSQLProcedureOutputsImpl( this );
	}

	public NoSQLProcedureCallMemento getMemento() {
		return memento;
	}
}
