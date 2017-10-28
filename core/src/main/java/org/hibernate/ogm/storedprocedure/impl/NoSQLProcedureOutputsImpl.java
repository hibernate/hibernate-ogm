/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.storedprocedure.impl;

import static org.hibernate.ogm.util.impl.TupleContextHelper.tupleContext;

import java.io.Serializable;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.ParameterMode;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.ogm.dialect.query.spi.ClosableIterator;
import org.hibernate.ogm.dialect.query.spi.QueryParameters;
import org.hibernate.ogm.dialect.query.spi.RowSelection;
import org.hibernate.ogm.dialect.query.spi.TypedGridValue;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.loader.impl.OgmLoadingContext;
import org.hibernate.ogm.loader.impl.TupleBasedEntityLoader;
import org.hibernate.ogm.model.spi.EntityMetadataInformation;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;
import org.hibernate.ogm.type.spi.TypeTranslator;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.procedure.ParameterRegistration;
import org.hibernate.procedure.ProcedureOutputs;
import org.hibernate.result.Output;
import org.hibernate.type.Type;

/**
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
public class NoSQLProcedureOutputsImpl implements ProcedureOutputs {
	private static final Log log = LoggerFactory.make();
	private static final Set<ParameterMode> SUPPORTED_MODES = EnumSet.of( ParameterMode.IN , ParameterMode.REF_CURSOR );
	private static final RowSelection ROW_SELECTION = new RowSelection( 1, 1 );
	private final NoSQLProcedureCallImpl procedureCall;
	private final TypeTranslator typeTranslator;
	private SessionFactoryImplementor sessionFactory;

	public NoSQLProcedureOutputsImpl(NoSQLProcedureCallImpl procedureCall) {
		this.procedureCall = procedureCall;
		this.sessionFactory = procedureCall.getSession().getFactory();
		this.typeTranslator = sessionFactory.getServiceRegistry().getService( TypeTranslator.class );
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

		QueryParameters queryParameters = null;
		List<TypedGridValue> positionalParameters = new LinkedList<>();
		Map<String, TypedGridValue> namedParameters = new HashMap<>();


		for ( ParameterRegistration parameterRegistration : procedureCall.getRegisteredParameters() ) {
			NoSQLProcedureParameterRegistration nosqlParameterRegistration = (NoSQLProcedureParameterRegistration) parameterRegistration;
			if ( SUPPORTED_MODES.contains( parameterRegistration.getMode() ) ) {
				if ( parameterRegistration.getMode() != ParameterMode.REF_CURSOR ) {
					Object value = nosqlParameterRegistration.getBind().getValue();
					Type hibernateType = nosqlParameterRegistration.getHibernateType();
					TypedGridValue typedGridValue = TypedGridValue.fromOrmTypedValue( new TypedValue( hibernateType, value ),
							typeTranslator, sessionFactory );
					if ( procedureCall.getGridDialect().supportsNamedPosition() ) {
						namedParameters.put( nosqlParameterRegistration.getName(), typedGridValue );
					}
					else {
						positionalParameters.add( typedGridValue );
					}
				}
				else {
					isResultRefCursor = true;
				}
			}
			else {
				throw new UnsupportedOperationException( String.format( "Parameter Mode %s not supported yet!", nosqlParameterRegistration.getMode() ) );
			}
		}

		queryParameters = new QueryParameters( ROW_SELECTION, namedParameters, positionalParameters, null );

		TupleContext tupleContext = null;
		OgmEntityPersister entityPersister = null;
		String entityName = null;

		if ( procedureCall.getMemento() != null ) {
			for ( String querySpace : procedureCall.getMemento().getSynchronizedQuerySpaces() ) {
				procedureCall.addSynchronizedQuerySpace( querySpace );
			}
		}

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

		ClosableIterator<Tuple> result = procedureCall.getGridDialect().callStoredProcedure( procedureCall.getProcedureName(), queryParameters, tupleContext );

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
		return new NoSQLProcedureOutputImpl( entityList, isResultRefCursor );
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
