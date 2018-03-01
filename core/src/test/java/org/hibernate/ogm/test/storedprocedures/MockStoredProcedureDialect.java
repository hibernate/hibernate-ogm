/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.storedprocedures;

import static org.hibernate.ogm.backendtck.storedprocedures.Car.UNIQUE_VALUE_PROC_PARAM;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hibernate.ogm.backendtck.storedprocedures.Car;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.query.spi.ClosableIterator;
import org.hibernate.ogm.dialect.spi.AssociationContext;
import org.hibernate.ogm.dialect.spi.AssociationTypeContext;
import org.hibernate.ogm.dialect.spi.BaseGridDialect;
import org.hibernate.ogm.dialect.spi.ModelConsumer;
import org.hibernate.ogm.dialect.spi.NextValueRequest;
import org.hibernate.ogm.dialect.spi.OperationContext;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.dialect.spi.TupleTypeContext;
import org.hibernate.ogm.dialect.storedprocedure.spi.StoredProcedureAwareGridDialect;
import org.hibernate.ogm.entityentry.impl.TuplePointer;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.storedprocedure.ProcedureQueryParameters;
import org.hibernate.ogm.util.impl.CollectionHelper;

/**
 * This dialect is used to test stored procedures calls.
 *
 * @see Car
 *
 * @author Davide D'Alto
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
public class MockStoredProcedureDialect extends BaseGridDialect implements StoredProcedureAwareGridDialect {

	public MockStoredProcedureDialect(DatastoreProvider provider) {
	}

	@Override
	public ClosableIterator<Tuple> callStoredProcedure(String storedProcedureName, ProcedureQueryParameters queryParameters, TupleContext tupleContext) {
		switch ( storedProcedureName ) {
			case Car.RESULT_SET_PROC:
				return callResultsetStoredProcedure( queryParameters );
			case Car.SIMPLE_VALUE_PROC:
				return callSimpleValueStoredProcedure( queryParameters );
			default:
				return CollectionHelper.newClosableIterator( Collections.emptyList() );
		}
	}

	private ClosableIterator<Tuple> callSimpleValueStoredProcedure(ProcedureQueryParameters queryParameters) {
		Integer simpleValueParam = getSimpleValue( queryParameters );
		if ( simpleValueParam == 1 ) {
			Tuple tuple = new Tuple();
			tuple.put( "", 1 );
			return CollectionHelper.newClosableIterator( Arrays.asList( tuple ) );
		}
		return CollectionHelper.newClosableIterator( Collections.emptyList() );
	}

	private static ClosableIterator<Tuple> callResultsetStoredProcedure(ProcedureQueryParameters queryParameters) {
		List<Tuple> tuples = new ArrayList<>();
		Integer carId = carId( queryParameters );
		String carTitle = carTitle( queryParameters );
		Tuple tuple = new Tuple();
		tuple.put( "id", carId );
		tuple.put( "title", carTitle );
		tuples.add( tuple );
		return CollectionHelper.newClosableIterator( tuples );
	}

	private static Integer getSimpleValue(ProcedureQueryParameters queryParameters) {
		if ( queryParameters.getNamedParameters().isEmpty() ) {
			return (Integer) queryParameters.getPositionalParameters().get( 0 );
		}
		return (Integer) queryParameters.getNamedParameters().get( UNIQUE_VALUE_PROC_PARAM );
	}

	private static Integer carId(ProcedureQueryParameters queryParameters) {
		if ( queryParameters.getNamedParameters().isEmpty() ) {
			return (Integer) queryParameters.getPositionalParameters().get( 0 );
		}
		return (Integer) queryParameters.getNamedParameters().get( Car.RESULT_SET_PROC_ID_PARAM );
	}

	private static String carTitle(ProcedureQueryParameters queryParameters) {
		if ( queryParameters.getNamedParameters().isEmpty() ) {
			return (String) queryParameters.getPositionalParameters().get( 1 );
		}
		return (String) queryParameters.getNamedParameters().get( Car.RESULT_SET_PROC_TITLE_PARAM );
	}

	public Tuple getTuple(EntityKey key, OperationContext tupleContext) {
		return null;
	}

	@Override
	public Tuple createTuple(EntityKey key, OperationContext tupleContext) {
		return null;
	}

	@Override
	public void insertOrUpdateTuple(EntityKey key, TuplePointer tuplePointer, TupleContext tupleContext) {
	}

	@Override
	public void removeTuple(EntityKey key, TupleContext tupleContext) {
	}

	@Override
	public Association getAssociation(AssociationKey key, AssociationContext associationContext) {
		return null;
	}

	@Override
	public Association createAssociation(AssociationKey key, AssociationContext associationContext) {
		return null;
	}

	@Override
	public void insertOrUpdateAssociation(AssociationKey key, Association association, AssociationContext associationContext) {
	}

	@Override
	public void removeAssociation(AssociationKey key, AssociationContext associationContext) {
	}

	@Override
	public boolean isStoredInEntityStructure(AssociationKeyMetadata associationKeyMetadata, AssociationTypeContext associationTypeContext) {
		return false;
	}

	@Override
	public Number nextValue(NextValueRequest request) {
		return null;
	}

	@Override
	public void forEachTuple(ModelConsumer consumer, TupleTypeContext tupleTypeContext, EntityKeyMetadata entityKeyMetadata) {
	}
}
