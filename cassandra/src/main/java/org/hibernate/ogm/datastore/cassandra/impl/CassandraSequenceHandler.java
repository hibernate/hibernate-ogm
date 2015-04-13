/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.cassandra.impl;

import java.util.ArrayList;
import java.util.List;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.exceptions.DriverException;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;

import org.hibernate.ogm.dialect.spi.NextValueRequest;
import org.hibernate.ogm.model.key.spi.IdSourceKey;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static com.datastax.driver.core.querybuilder.QueryBuilder.set;
import static com.datastax.driver.core.querybuilder.QueryBuilder.update;

/**
 * Abstraction layer for adding id sequence numbers support to Cassandra,
 * which lacks native auto-increment columns or sequences.
 *
 * @author Jonathan Halliday
 */
public class CassandraSequenceHandler {

	private final CassandraDatastoreProvider provider;

	public CassandraSequenceHandler(CassandraDatastoreProvider provider) {
		this.provider = provider;
	}

	public void createSequence(String generatorsKey, CassandraDatastoreProvider datastoreProvider) {

		// ogm wiring doesn't allow us to get at the real col names :-(
		List<String> primaryKeyName = new ArrayList<String>( 1 );
		primaryKeyName.add( "sequence_name" );
		List<String> columnNames = new ArrayList<String>( 2 );
		columnNames.add( "sequence_name" );
		columnNames.add( "sequence_value" );
		List<String> columnTypes = new ArrayList<String>( 2 );
		columnTypes.add( "varchar" );
		columnTypes.add( "bigint" );
		datastoreProvider.createColumnFamilyIfNeeded( generatorsKey, primaryKeyName, columnNames, columnTypes );
	}

	private Long nextValueSelect(String tableName, String sequenceName) {

		Statement select = select().column( "sequence_value" ).from( "\"" + tableName + "\"" )
				.where( eq( "sequence_name", QueryBuilder.bindMarker() ) );

		PreparedStatement preparedStatement = provider.getSession().prepare( select.toString() );
		BoundStatement boundStatement = preparedStatement.bind( sequenceName );

		ResultSet resultSet;
		try {
			resultSet = provider.getSession().execute( boundStatement );
		}
		catch (DriverException e) {
			System.out.println( e.toString() );
			throw e;
		}

		if ( resultSet.isExhausted() ) {
			return null;
		}
		else {
			return resultSet.one().getLong( 0 );
		}
	}

	private Long nextValueInsert(String tableName, String sequenceName, Long value) {

		Insert insert = insertInto( "\"" + tableName + "\"" )
				.value( "sequence_name", QueryBuilder.bindMarker( "sequence_name" ) )
				.value( "sequence_value", QueryBuilder.bindMarker( "sequence_value" ) )
				.ifNotExists();

		PreparedStatement preparedStatement = provider.getSession().prepare( insert.toString() );
		BoundStatement boundStatement = preparedStatement.bind();
		boundStatement.setString( "sequence_name", sequenceName );
		boundStatement.setLong( "sequence_value", value );

		try {
			provider.getSession().execute( boundStatement );
		}
		catch (DriverException e) {
			System.out.println( e.toString() );
			throw e;
		}

		return nextValueSelect( tableName, sequenceName );
	}

	private boolean nextValueUpdate(String tableName, String sequenceName, Long oldValue, Long newValue) {

		Statement update = update( "\"" + tableName + "\"" )
				.with( set( "sequence_value", QueryBuilder.bindMarker( "sequence_value_new" ) ) )
				.where( eq( "sequence_name", QueryBuilder.bindMarker( "sequence_name" ) ) )
				.onlyIf( eq( "sequence_value", QueryBuilder.bindMarker( "sequence_value_old" ) ) );

		PreparedStatement preparedStatement = provider.getSession().prepare( update.toString() );
		BoundStatement boundStatement = preparedStatement.bind();
		boundStatement.setString( "sequence_name", sequenceName );
		boundStatement.setLong( "sequence_value_new", newValue );
		boundStatement.setLong( "sequence_value_old", oldValue );

		ResultSet resultSet;
		try {
			resultSet = provider.getSession().execute( boundStatement );
		}
		catch (DriverException e) {
			System.out.println( e.toString() );
			throw e;
		}

		return resultSet.one().getBool( 0 );
	}

	public Number nextValue(NextValueRequest request) {

		IdSourceKey key = request.getKey();
		Long valueFromDb = null;
		boolean done = false;
		do {
			valueFromDb = nextValueSelect( key.getTable(), key.getColumnValues()[0].toString() );

			if ( valueFromDb == null ) {
				//if not there, insert initial value
				valueFromDb = nextValueInsert(
						key.getTable(),
						key.getColumnValues()[0].toString(),
						(long) request.getInitialValue()
				);
			}

			//update seq value ready for the next reader
			Long updatedValue = valueFromDb + (long) request.getIncrement();
			done = nextValueUpdate( key.getTable(), key.getColumnValues()[0].toString(), valueFromDb, updatedValue );
		}
		while ( !done );

		return valueFromDb;
	}
}
