/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.utils;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.batch.spi.InsertOrUpdateAssociationOperation;
import org.hibernate.ogm.dialect.batch.spi.InsertOrUpdateTupleOperation;
import org.hibernate.ogm.dialect.batch.spi.Operation;
import org.hibernate.ogm.dialect.batch.spi.OperationsQueue;
import org.hibernate.ogm.dialect.batch.spi.RemoveAssociationOperation;
import org.hibernate.ogm.dialect.batch.spi.RemoveTupleOperation;
import org.hibernate.ogm.dialect.impl.ForwardingGridDialect;
import org.hibernate.ogm.dialect.query.spi.BackendQuery;
import org.hibernate.ogm.dialect.query.spi.ClosableIterator;
import org.hibernate.ogm.dialect.spi.AssociationContext;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.RowKey;
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.model.spi.Tuple;

/**
 * A testing dialect wrapper which logs all the invoked {@link GridDialect} operations.
 *
 * @author Gunnar Morling
 */
public class InvokedOperationsLoggingDialect extends ForwardingGridDialect<Serializable> {

	/**
	 * Path for the logging file to be created, relative to the current working directory.
	 */
	private static final String PATH = "invocations.log";

	/**
	 * Set to {@code true} if a log with all the ops and their parameters should be written into the file specified
	 * above.
	 */
	private final boolean writeFile = false;

	private int opIndex = 0;
	private Path file;
	private final List<String> operations = new ArrayList<String>();

	public InvokedOperationsLoggingDialect(DatastoreProvider provider) {
		super( TestHelper.getCurrentGridDialect( provider ) );

		if ( writeFile ) {
			resetFile();
		}
	}

	public List<String> getOperations() {
		return Collections.unmodifiableList( operations );
	}

	public void reset() {
		operations.clear();
		if ( writeFile ) {
			resetFile();
		}
	}

	@Override
	public Tuple getTuple(EntityKey key, TupleContext tupleContext) {
		Tuple tuple = super.getTuple( key, tupleContext );
		log( "getTuple", key.toString(), tuple != null ? tuple.toString() : "null" );
		return tuple;
	}

	@Override
	public Tuple createTuple(EntityKey key, TupleContext tupleContext) {
		Tuple tuple = super.createTuple( key, tupleContext );
		log( "createTuple", key.toString(), tuple != null ? tuple.toString() : "null" );
		return tuple;
	}

	@Override
	public void insertOrUpdateTuple(EntityKey key, Tuple tuple, TupleContext tupleContext) {
		super.insertOrUpdateTuple( key, tuple, tupleContext );
		log( "insertOrUpdateTuple", key.toString() + ", " + tuple.toString(), "VOID" );
	}

	@Override
	public void removeTuple(EntityKey key, TupleContext tupleContext) {
		super.removeTuple( key, tupleContext );
		log( "removeTuple", key.toString(), "VOID" );
	}

	@Override
	public Association getAssociation(AssociationKey key, AssociationContext associationContext) {
		Association association = super.getAssociation( key, associationContext );
		log( "getAssociation", key.toString(), toShortString( association ) );
		return association;
	}

	@Override
	public Association createAssociation(AssociationKey key, AssociationContext associationContext) {
		Association association = super.createAssociation( key, associationContext );
		log( "createAssociation", key.toString(), toShortString( association ) );
		return association;
	}

	@Override
	public void insertOrUpdateAssociation(AssociationKey key, Association association, AssociationContext associationContext) {
		super.insertOrUpdateAssociation( key, association, associationContext );
		log( "insertOrUpdateAssociation", key.toString() + ", " + toShortString( association ), "VOID" );
	}

	@Override
	public void removeAssociation(AssociationKey key, AssociationContext associationContext) {
		super.removeAssociation( key, associationContext );
		log( "removeAssociation", key.toString(), "VOID" );
	}

	@Override
	public boolean updateTupleWithOptimisticLock(EntityKey entityKey, Tuple oldLockState, Tuple tuple, TupleContext tupleContext) {
		boolean success = super.updateTupleWithOptimisticLock( entityKey, oldLockState, tuple, tupleContext );
		log( "updateTuple", entityKey.toString() + ", " + tuple.toString(), String.valueOf( success ) );
		return success;
	}

	@Override
	public boolean removeTupleWithOptimisticLock(EntityKey entityKey, Tuple oldLockState, TupleContext tupleContext) {
		boolean success = super.removeTupleWithOptimisticLock( entityKey, oldLockState, tupleContext );
		log( "removeTuple", entityKey.toString(), String.valueOf( success ) );
		return success;
	}

	@Override
	public Tuple createTuple(EntityKeyMetadata entityKeyMetadata, TupleContext tupleContext) {
		return super.createTuple( entityKeyMetadata, tupleContext );
	}

	@Override
	public void insertTuple(EntityKeyMetadata entityKeyMetadata, Tuple tuple, TupleContext tupleContext) {
		super.insertTuple( entityKeyMetadata, tuple, tupleContext );
		log( "insertTuple", entityKeyMetadata.toString() + ", " + tuple.toString(), "VOID" );
	}

	@Override
	public ClosableIterator<Tuple> executeBackendQuery(BackendQuery<Serializable> query, QueryParameters queryParameters) {
		ClosableIterator<Tuple> result = super.executeBackendQuery( query, queryParameters );
		log( "executeBackendQuery", query.toString() + ", " + queryParameters.toString(), "tbd." );
		return result;
	}

	@Override
	public void executeBatch(OperationsQueue queue) {
		OperationsQueue newQueue = new OperationsQueue();
		StringBuilder sb = new StringBuilder();

		if ( !queue.isClosed() ) {
			Operation operation = queue.poll();
			while ( operation != null ) {
				newQueue.add( operation );

				if ( operation instanceof InsertOrUpdateTupleOperation ) {
					sb.append( "InsertOrUpdateTuple(" ).append( ( (InsertOrUpdateTupleOperation) operation ).getEntityKey() ).append( " )" );
				}
				else if ( operation instanceof RemoveTupleOperation ) {
					sb.append( "RemoveTuple(" ).append( ( (RemoveTupleOperation) operation ).getEntityKey() ).append( " )" );
				}
				else if ( operation instanceof InsertOrUpdateAssociationOperation ) {
					sb.append( "InsertOrUpdateAssociation(" ).append( ( (InsertOrUpdateAssociationOperation) operation ).getAssociationKey() ).append( " )" );
				}
				else if ( operation instanceof RemoveAssociationOperation ) {
					sb.append( "RemoveAssociation(" ).append( ( (RemoveAssociationOperation) operation ).getAssociationKey() ).append( " )" );
				}

				operation = queue.poll();

				if ( operation != null ) {
					sb.append( ", " );
				}
			}
		}

		super.executeBatch( newQueue );

		log( "executeBatch", sb.toString(), "VOID" );
	}

	private void log(String operation, String parameters, String returnValue) {
		operations.add( operation );

		if ( !writeFile ) {
			return;
		}

		String line = opIndex + "\t| " + operation + "\t| " + parameters + "\t| " + returnValue + System.lineSeparator();
		try {
			Files.write( file, line.getBytes(), StandardOpenOption.APPEND );
		}
		catch (IOException e) {
			throw new RuntimeException( e );
		}

		opIndex++;
	}

	private String toShortString(Association association) {
		if ( association == null ) {
			return null;
		}

		StringBuilder sb = new StringBuilder( "Association[");
		int i = 0;
		for ( RowKey rowKey : association.getKeys() ) {
			sb.append( toShortString( rowKey ) ).append( "=" ).append( toShortString( association.get( rowKey ) ) );
			i++;
			if ( i < association.getKeys().size() ) {
				sb.append( "," );
			}
		}

		sb.append( "]" );
		return sb.toString();
	}

	private String toShortString(RowKey rowKey) {
		final StringBuilder sb = new StringBuilder();
		sb.append( "(" );
		int i = 0;
		for ( String column : rowKey.getColumnNames() ) {
			sb.append( column ).append( "=" ).append( rowKey.getColumnValue( column ) );
			i++;
			if ( i < rowKey.getColumnNames().length ) {
				sb.append( ", " );
			}
		}
		sb.append( ")" );
		return sb.toString();
	}

	private String toShortString(Tuple tuple) {
		StringBuilder sb = new StringBuilder( "(");
		int i = 0;
		for ( String column : tuple.getColumnNames() ) {
			sb.append( column ).append( "=" ).append( tuple.get( column ) );
			i++;
			if ( i < tuple.getColumnNames().size() ) {
				sb.append( ", " );
			}
		}

		sb.append( ")" );
		return sb.toString();
	}

	private void resetFile() {
		file = Paths.get( PATH );
		try {
			if ( !Files.exists( file ) ) {
				file = Files.createFile( file );
			}
			Files.write( file, ("#\top\tparameters\tresult" + System.lineSeparator() ).getBytes(), StandardOpenOption.TRUNCATE_EXISTING );
		}
		catch (IOException e) {
			throw new RuntimeException( e );
		}
	}
}
