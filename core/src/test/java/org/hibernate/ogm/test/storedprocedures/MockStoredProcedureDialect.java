/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.storedprocedures;

import static org.hibernate.ogm.backendtck.storedprocedures.Car.TEST_SIMPLE_VALUE_STORED_PROC_PARAM_NAME;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 * @author Davide D'Alto
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
public class MockStoredProcedureDialect extends BaseGridDialect implements StoredProcedureAwareGridDialect {

	private static final Map<Integer, Car> CARS_DB;

	static {
		Map<Integer, Car> cars = new HashMap<>();
		cars.put( 1, new Car( 1, "title" ) );
		cars.put( 2, new Car( 2, "title'1" ) );
		cars.put( 3, new Car( 3, "title'3" ) );
		cars.put( 4, new Car( 4, "title'4" ) );
		CARS_DB = Collections.unmodifiableMap( cars );
	}

	public MockStoredProcedureDialect(DatastoreProvider provider) {
	}

	@Override
	public ClosableIterator<Tuple> callStoredProcedure(String storedProcedureName, ProcedureQueryParameters queryParameters, TupleContext tupleContext) {
		switch ( storedProcedureName ) {
			case Car.TEST_RESULT_SET_STORED_PROC:
				return callResultsetStoredProcedure( queryParameters );
			case Car.TEST_SIMPLE_VALUE_STORED_PROC:
				return callSimpleValueStoredProcedure( queryParameters );
			default:
				return CollectionHelper.newClosableIterator( Collections.emptyList() );
		}
	}

	private ClosableIterator<Tuple> callSimpleValueStoredProcedure(ProcedureQueryParameters queryParameters) {
		List<Tuple> tuples = new ArrayList<>();
		Integer simpleValueParam = getSimpleValue( queryParameters );
		if ( simpleValueParam == 1 ) {
			Tuple tuple = new Tuple();
			tuple.put( "", 1 );
			tuples = new ArrayList<>();
			tuples.add( tuple );
		}
		return CollectionHelper.newClosableIterator( tuples );
	}

	private static ClosableIterator<Tuple> callResultsetStoredProcedure(ProcedureQueryParameters queryParameters) {
		List<Tuple> tuples = new ArrayList<>();
		Integer carId = getCarId( queryParameters );
		Car result = CARS_DB.get( carId );
		Tuple tuple = new Tuple();
		if ( result != null ) {
			String carTitle = getCarTitle( queryParameters );
			Car expected = new Car( carId, carTitle );
			if ( result.equals( expected ) ) {
				tuple.put( "id", result.getId() );
				tuple.put( "title", result.getTitle() );
				tuples.add( tuple );
			}
		}
		return CollectionHelper.newClosableIterator( tuples );
	}

	private static Integer getSimpleValue(ProcedureQueryParameters queryParameters) {
		if ( queryParameters.getNamedParameters().isEmpty() ) {
			return (Integer) queryParameters.getPositionalParameters().get( 0 );
		}
		return (Integer) queryParameters.getNamedParameters().get( TEST_SIMPLE_VALUE_STORED_PROC_PARAM_NAME );
	}

	private static Integer getCarId(ProcedureQueryParameters queryParameters) {
		if ( queryParameters.getNamedParameters().isEmpty() ) {
			return (Integer) queryParameters.getPositionalParameters().get( 0 );
		}
		return (Integer) queryParameters.getNamedParameters().get( Car.TEST_RESULT_SET_STORED_PROC_ID_PARAM_NAME );
	}

	private static String getCarTitle(ProcedureQueryParameters queryParameters) {
		if ( queryParameters.getNamedParameters().isEmpty() ) {
			return (String) queryParameters.getPositionalParameters().get( 1 );
		}
		return (String) queryParameters.getNamedParameters().get( Car.TEST_RESULT_SET_STORED_PROC_TITLE_PARAM_NAME );
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
