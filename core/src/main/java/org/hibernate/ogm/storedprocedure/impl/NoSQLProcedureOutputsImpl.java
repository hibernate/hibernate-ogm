/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.storedprocedure.impl;

import static org.hibernate.ogm.util.impl.CustomLoaderHelper.getTuplesAsList;
import static org.hibernate.ogm.util.impl.CustomLoaderHelper.listOfEntities;
import static org.hibernate.ogm.util.impl.TupleContextHelper.tupleContext;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.ParameterMode;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.dialect.query.spi.ClosableIterator;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.dialect.storedprocedure.spi.StoredProcedureAwareGridDialect;
import org.hibernate.ogm.model.spi.EntityMetadataInformation;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;
import org.hibernate.ogm.storedprocedure.ProcedureQueryParameters;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.procedure.ParameterRegistration;
import org.hibernate.procedure.ProcedureOutputs;
import org.hibernate.procedure.internal.NoSQLProcedureCallImpl;
import org.hibernate.result.Output;

/**
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
public class NoSQLProcedureOutputsImpl implements ProcedureOutputs {

	private final NoSQLProcedureCallImpl procedureCall;
	private final SessionFactoryImplementor sessionFactory;
	private StoredProcedureAwareGridDialect gridDialect;

	public NoSQLProcedureOutputsImpl(NoSQLProcedureCallImpl procedureCall) {
		this.procedureCall = procedureCall;
		this.sessionFactory = procedureCall.getSession().getFactory();
		this.gridDialect = this.sessionFactory.getServiceRegistry().getService( StoredProcedureAwareGridDialect.class );
	}

	@Override
	public <T> T getOutputParameterValue(ParameterRegistration<T> parameterRegistration) {
		throw new UnsupportedOperationException( "Out parameters not supported yet!" );
	}

	@Override
	public Object getOutputParameterValue(String name) {
		throw new UnsupportedOperationException( "Out parameters not supported yet!" );
	}

	@Override
	public Object getOutputParameterValue(int position) {
		throw new UnsupportedOperationException( "Out parameters not supported yet!" );
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Output getCurrent() {
		// the result can be entity or single value result
		boolean isResultRefCursor = false;
		List<?> entityList = null;

		List<Object> positionalParameters = new ArrayList<>();
		Map<String, Object> namedParameters = new HashMap<>();
		for ( ParameterRegistration nosqlParameterRegistration : procedureCall.getRegisteredParameters() ) {
			if ( nosqlParameterRegistration.getMode() != ParameterMode.REF_CURSOR ) {
				Object value = nosqlParameterRegistration.getBind().getValue();
				if ( nosqlParameterRegistration.getName() != null ) {
					namedParameters.put( nosqlParameterRegistration.getName(), value );
				}
				else if ( nosqlParameterRegistration.getPosition() != null ) {
					positionalParameters.add( value );
				}
			}
			else {
				isResultRefCursor = true;
			}
		}

		ProcedureQueryParameters queryParameters = new ProcedureQueryParameters( namedParameters, positionalParameters );

		TupleContext tupleContext = null;
		OgmEntityPersister entityPersister = null;
		String entityName = null;

		if ( !procedureCall.getSynchronizedQuerySpaces().isEmpty() ) {
			String querySpace = procedureCall.getSynchronizedQuerySpaces().iterator().next();

			for ( Map.Entry<String, EntityPersister> entry : procedureCall.getSession().getFactory().getEntityPersisters().entrySet() ) {
				List<Serializable> querySpaces = Arrays.asList( entry.getValue().getQuerySpaces() );
				if ( querySpaces.contains( querySpace ) ) {
					entityPersister = (OgmEntityPersister) entry.getValue();
					entityName = entry.getKey();
				}
			}
			tupleContext = tupleContext( procedureCall.getSession(), new EntityMetadataInformation( entityPersister.getEntityKeyMetadata(), entityName ) );
		}

		ClosableIterator<Tuple> result = gridDialect.callStoredProcedure( procedureCall.getProcedureName(), queryParameters, tupleContext );

		if ( !procedureCall.getSynchronizedQuerySpaces().isEmpty() ) {
			entityList = listOfEntities( procedureCall.getSession(), entityPersister.getMappedClass(), result );
		}
		else if ( result == null ) {
			// call a procedure without result
			return null;
		}
		else {
			entityList = getTuplesAsList( result );
		}

		//copy data from iterator
		return new NoSQLProcedureResultSetOutputImpl( entityList, isResultRefCursor );
	}

	@Override
	public boolean goToNext() {
		return false;
	}

	@Override
	public void release() {
	}

	public boolean isResultSet() {
		return getCurrent().isResultSet();
	}
}
