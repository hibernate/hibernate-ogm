/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.storedprocedure.impl;

import org.hibernate.procedure.ParameterRegistration;
import org.hibernate.procedure.ProcedureOutputs;
import org.hibernate.result.Output;

/**
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
public class NoSQLProcedureOutputsImpl implements ProcedureOutputs {
	private final NoSQLProcedureCallImpl procedureCall;

	public NoSQLProcedureOutputsImpl(NoSQLProcedureCallImpl procedureCall) {
		this.procedureCall = procedureCall;
	}

	@Override
	public <T> T getOutputParameterValue(ParameterRegistration<T> parameterRegistration) {
		return null;
	}

	@Override
	public Object getOutputParameterValue(String name) {
		//return procedureCall.getParameterRegistration( name ).extract( callableStatement );
		return null;
	}

	@Override
	public Object getOutputParameterValue(int position) {
		//return procedureCall.getParameterRegistration( position ).extract( callableStatement );
		return null;
	}

	@Override
	public Output getCurrent() {
		return new NoSQLProcedureOutputImpl();
	}

	@Override
	public boolean goToNext() {
		return false;
	}

	@Override
	public void release() {

	}
}
