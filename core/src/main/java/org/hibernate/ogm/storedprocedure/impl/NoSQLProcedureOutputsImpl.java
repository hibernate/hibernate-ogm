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
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.ParameterMode;
import javax.persistence.TemporalType;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.ogm.dialect.query.spi.ClosableIterator;
import org.hibernate.ogm.dialect.query.spi.QueryParameters;
import org.hibernate.ogm.dialect.query.spi.RowSelection;
import org.hibernate.ogm.dialect.query.spi.TypedGridValue;
import org.hibernate.ogm.dialect.spi.TupleContext;
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
import org.hibernate.type.TimestampType;
import org.hibernate.type.Type;

/**
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
public class NoSQLProcedureOutputsImpl implements ProcedureOutputs {
	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );
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

	private Object processByTemporalType(TemporalType temporalType, Object value ) {
		Calendar calendar = null;
		if ( value instanceof Calendar ) {
			calendar = (Calendar) value;
		}
		else {
			calendar = Calendar.getInstance();
			calendar.setTime( (Date) value );
		}
		switch ( temporalType ) {
			case TIMESTAMP:
				break;
			case DATE:
				//remove time part.
				calendar.set( Calendar.HOUR_OF_DAY,0 );
				calendar.set( Calendar.MINUTE,0 );
				calendar.set( Calendar.SECOND,0 );
				calendar.set( Calendar.MILLISECOND,0 );
				break;
			case TIME:
				//remove date part
				//see https://docs.oracle.com/javase/6/docs/api/java/sql/Time.html
				calendar.set( Calendar.YEAR,1970 );
				calendar.set( Calendar.DAY_OF_YEAR, 1 );
		}
		/*
		java.lang.ClassCastException: java.util.GregorianCalendar cannot be cast to java.util.Date
	at org.hibernate.type.descriptor.java.JdbcTimestampTypeDescriptor.unwrap(JdbcTimestampTypeDescriptor.java:24)
	at org.hibernate.ogm.type.descriptor.impl.PassThroughGridTypeDescriptor$1.doBind(PassThroughGridTypeDescriptor.java:26)
	at org.hibernate.ogm.type.descriptor.impl.BasicGridBinder.bind(BasicGridBinder.java:65)
	at org.hibernate.ogm.type.impl.AbstractGenericBasicType.nullSafeSet(AbstractGenericBasicType.java:239)
	at org.hibernate.ogm.type.impl.AbstractGenericBasicType.nullSafeSet(AbstractGenericBasicType.java:234)
	at org.hibernate.ogm.type.impl.AbstractGenericBasicType.convertToBackendType(AbstractGenericBasicType.java:256)
	at org.hibernate.ogm.dialect.query.spi.TypedGridValue.fromOrmTypedValue(TypedGridValue.java:32)
		 */
		// convert to Data in any way
		return calendar.getTime();
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
					TemporalType temporalType = nosqlParameterRegistration.getBind().getExplicitTemporalType();
					if ( hibernateType instanceof TimestampType ) {
						//need a process date
						value = processByTemporalType( temporalType, value );
					}
					TypedGridValue typedGridValue = TypedGridValue.fromOrmTypedValue( new TypedValue( hibernateType, value ), typeTranslator, sessionFactory );

					if ( procedureCall.getGridDialect().supportsNamedParameters() ) {
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

		queryParameters = new QueryParameters( ROW_SELECTION, namedParameters, positionalParameters, Collections.<String>emptyList() );

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
		return new NoSQLProcedureResultSetOutputImpl( entityList, isResultRefCursor );
	}

	private Date convert(Date date, TemporalType temporalType) {
		return date;
	}

	private Calendar convert(Calendar calendar, TemporalType temporalType) {
		return calendar;
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
