/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.storedprocedure.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.persistence.ParameterMode;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.ogm.dialect.query.spi.ClosableIterator;
import org.hibernate.ogm.loader.impl.OgmLoadingContext;
import org.hibernate.ogm.loader.impl.TupleBasedEntityLoader;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.procedure.ParameterRegistration;
import org.hibernate.procedure.ProcedureOutputs;
import org.hibernate.result.Output;

/**
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
public class NoSQLProcedureOutputsImpl implements ProcedureOutputs {
	private static final Log log = LoggerFactory.make();
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
		// the result can be entity or single value result
		List<?> entityList = null;

		if ( procedureCall.getGridDialect().supportsNamedPosition() ) {
//

		}
		else {
			List parameters = new ArrayList( procedureCall.getRegisteredParameters().size() );
			for ( ParameterRegistration parameterRegistration : procedureCall.getRegisteredParameters() ) {
				//@todo will we supports out and in_out parameters?
				if ( parameterRegistration.getMode() != ParameterMode.REF_CURSOR ) {
					parameters.add( parameterRegistration.getBind().getValue() );
				}
			}
			log.infof( "indexed parameters: %s", parameters );
			ClosableIterator<Tuple> result = procedureCall.getGridDialect().callStoredProcedure( procedureCall.getProcedureName(),
																		 parameters.toArray() , null );

			if ( !procedureCall.getSynchronizedQuerySpaces().isEmpty() ) {
				String querySpace = procedureCall.getSynchronizedQuerySpaces().iterator().next();
				EntityPersister entityPersister = null;
				for ( Map.Entry<String, EntityPersister> entry : procedureCall.getSession().getFactory()
						.getEntityPersisters().entrySet() ) {
					log.infof( "getEntityName: %s", entry.getValue().getEntityName() );
					log.infof( "getRootEntityName: %s", entry.getValue().getRootEntityName() );
					log.infof( "getQuerySpaces(): %s", entry.getValue().getQuerySpaces() );

					List<Serializable> querySpaces = Arrays.asList( entry.getValue().getQuerySpaces() );

					if ( querySpaces.contains( querySpace ) ) {
						entityPersister = entry.getValue();
					}

				}

				entityList = listOfEntities( procedureCall.getSession(), entityPersister.getMappedClass(), result );
			}
			else if ( result == null ) {
				//call a procedure without result
				return null;

			}
			else {
				entityList = getTuplesAsList( result );
			}


		}
		//copy data from iterator
		return new NoSQLProcedureOutputImpl( entityList );
	}
	//@todo dublicate code from BackendCustomLoader
	private List<Object> listOfEntities(SessionImplementor session, Class<?> returnedClass, ClosableIterator<Tuple> tuples) {
		TupleBasedEntityLoader loader = getLoader( session, returnedClass );
		OgmLoadingContext ogmLoadingContext = new OgmLoadingContext();
		ogmLoadingContext.setTuples( getTuplesAsList( tuples ) );
		return loader.loadEntitiesFromTuples( session, LockOptions.NONE, ogmLoadingContext );
	}

	private List<Tuple> getTuplesAsList(ClosableIterator<Tuple> tuples) {
		List<Tuple> tuplesAsList = new LinkedList<>();
		while ( tuples.hasNext() ) {
			tuplesAsList.add( tuples.next() );
		}
		return tuplesAsList;
	}
	private TupleBasedEntityLoader getLoader(SessionImplementor session, Class<?> entityClass) {
		OgmEntityPersister persister = (OgmEntityPersister) ( session.getFactory() ).getEntityPersister( entityClass.getName() );
		TupleBasedEntityLoader loader = (TupleBasedEntityLoader) persister.getAppropriateLoader( LockOptions.READ, session );
		return loader;
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
