/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.storedprocedure.impl;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.ogm.dialect.storedprocedure.spi.StoredProcedureGridDialect;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.procedure.ProcedureOutputs;
import org.hibernate.procedure.internal.ProcedureCallImpl;

/**
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
public class NoSQLProcedureCallImpl extends ProcedureCallImpl {
	private static final Log log = LoggerFactory.make();

	public NoSQLProcedureCallImpl(SessionImplementor session, String procedureName) {
		super( session, procedureName );
	}

	public NoSQLProcedureCallImpl(SessionImplementor session, String procedureName, Class[] resultClasses) {
		super( session, procedureName, resultClasses );
	}

	public NoSQLProcedureCallImpl(SessionImplementor session, String procedureName, String... resultSetMappings) {
		super( session, procedureName, resultSetMappings );
	}

	@Override
	public ProcedureOutputs getOutputs() {
		log.info( "I am here!" );
		StoredProcedureGridDialect gridDialect = getSession().getFactory().getServiceRegistry().getService(
				StoredProcedureGridDialect.class );

		log.infof( "gridDialect : %s", gridDialect );
		getQueryParameters().traceParameters( getSession().getFactory() );
		gridDialect.callStoredProcedure( getProcedureName(),null );
		//BackendStoredProcLoader loader = new BackendStoredProcLoader(  );

		return new NoSQLProcedureOutputsImpl( this );
	}
}
