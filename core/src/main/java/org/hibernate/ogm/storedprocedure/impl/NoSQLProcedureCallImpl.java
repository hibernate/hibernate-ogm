/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.storedprocedure.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.ParameterMode;

import org.hibernate.QueryException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.ogm.dialect.storedprocedure.spi.StoredProcedureAwareGridDialect;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.procedure.ParameterRegistration;
import org.hibernate.procedure.ProcedureOutputs;
import org.hibernate.procedure.internal.ProcedureCallImpl;
import org.hibernate.procedure.internal.ProcedureCallMementoImpl;
import org.hibernate.procedure.spi.ParameterStrategy;

/**
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
public class NoSQLProcedureCallImpl extends ProcedureCallImpl {
	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private ParameterStrategy parameterStrategy = ParameterStrategy.UNKNOWN;
	private List<NoSQLProcedureParameterRegistration<?>> registeredParameters = new ArrayList<NoSQLProcedureParameterRegistration<?>>();
	private StoredProcedureAwareGridDialect gridDialect;
	private ProcedureCallMementoImpl memento;

	public NoSQLProcedureCallImpl(SessionImplementor session, String procedureName) {
		super( session, procedureName );
		this.gridDialect = getGridDialectService();
		defineParameterStrategy();
	}

	public NoSQLProcedureCallImpl(SessionImplementor session, String procedureName, Class[] resultClasses) {
		super( session, procedureName, resultClasses );
		this.gridDialect = getGridDialectService();
		defineParameterStrategy();
	}

	public NoSQLProcedureCallImpl(SessionImplementor session, String procedureName, String... resultSetMappings) {
		super( session, procedureName, resultSetMappings );
		this.gridDialect = getGridDialectService();
	}

	private void defineParameterStrategy() {
		if ( this.gridDialect.supportsNamedParameters() ) {
			parameterStrategy = ParameterStrategy.NAMED;
		}
		else {
			parameterStrategy = ParameterStrategy.POSITIONAL;
		}
	}

	@Override
	public List<ParameterRegistration> getRegisteredParameters() {
		List<ParameterRegistration> resList = new ArrayList<>( registeredParameters.size() );
		for ( NoSQLProcedureParameterRegistration reg : registeredParameters ) {
			resList.add( reg );
		}
		return resList;
	}

	@Override
	public <T> ParameterRegistration<T> registerParameter(String name, Class<T> type, ParameterMode mode) {
		NoSQLProcedureParameterRegistration<T> parameterRegistration = new
				NoSQLProcedureParameterRegistration<T>( this, null, name, mode, type );
		registerParameter( parameterRegistration );
		return parameterRegistration;
	}

	@Override
	public <T> ParameterRegistration<T> registerParameter(int position, Class<T> type, ParameterMode mode) {
		NoSQLProcedureParameterRegistration<T> parameterRegistration =
				new NoSQLProcedureParameterRegistration<T>( this, position, null, mode, type );
		registerParameter( parameterRegistration );
		return parameterRegistration;
	}

	@Override
	public ProcedureOutputs getOutputs() {
		return new NoSQLProcedureOutputsImpl( this );
	}

	private void registerParameter(NoSQLProcedureParameterRegistration<?> parameter) {
		if ( StringHelper.isNotEmpty( parameter.getName() ) ) {
			prepareForNamedParameters();
		}
		else if ( parameter.getPosition() != null ) {
			prepareForPositionalParameters();
		}
		else {
			throw new IllegalArgumentException( "Given parameter did not define name or position [" + parameter + "]" );
		}
		registeredParameters.add( parameter );
	}

	private void prepareForPositionalParameters() {
		if ( parameterStrategy == ParameterStrategy.NAMED ) {
			throw new QueryException( "Cannot mix named and positional parameters" );
		}
	}

	private void prepareForNamedParameters() {
		if ( parameterStrategy == ParameterStrategy.POSITIONAL ) {
			throw new QueryException( "Cannot mix named and positional parameters" );
		}
	}

	private StoredProcedureAwareGridDialect getGridDialectService() {
		return getSession().getFactory().getServiceRegistry().getService( StoredProcedureAwareGridDialect.class );
	}

	public StoredProcedureAwareGridDialect getGridDialect() {
		return gridDialect;
	}

	public ProcedureCallMementoImpl getMemento() {
		return memento;
	}

	public void setMemento(ProcedureCallMementoImpl memento) {
		this.memento = memento;
	}
}
