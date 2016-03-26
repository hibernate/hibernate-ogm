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
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.quote;
import static com.datastax.driver.core.querybuilder.QueryBuilder.set;

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

	public void createSequence(IdSourceKeyMetadata metadata, CassandraDatastoreProvider datastoreProvider) {

		List<String> primaryKeyName = new ArrayList<String>( 1 );
		primaryKeyName.add( metadata.getKeyColumnName() );
		List<String> columnNames = new ArrayList<String>( 2 );
		columnNames.add( metadata.getKeyColumnName() );
		columnNames.add( metadata.getValueColumnName() );
		List<String> columnTypes = new ArrayList<String>( 2 );
		columnTypes.add( "varchar" );
		columnTypes.add( "bigint" );
		datastoreProvider.createColumnFamilyIfNeeded( metadata.getName(), primaryKeyName, columnNames, columnTypes );
	}

	private Long nextValueSelect(IdSourceKeyMetadata metadata, String sequenceName) {

		Statement select = provider.getQueryBuilder().select().column( quote( metadata.getValueColumnName() )  )
				.from( quote( metadata.getName() ) )
				.where( eq( metadata.getKeyColumnName(), QueryBuilder.bindMarker() ) );

		PreparedStatement preparedStatement = provider.getSession().prepare( select.toString() );
		BoundStatement boundStatement = preparedStatement.bind( sequenceName );

		ResultSet resultSet;
		try {
			resultSet = provider.getSession().execute( boundStatement );
		}
		catch (DriverException e) {
			throw e;
		}

		if ( resultSet.isExhausted() ) {
			return null;
		}
		else {
			return resultSet.one().getLong( 0 );
		}
	}

	private Long nextValueInsert(IdSourceKeyMetadata metadata, String sequenceName, Long value) {

		Insert insert = provider.getQueryBuilder().insertInto( quote( metadata.getName() ) )
				.value( quote( metadata.getKeyColumnName() ), QueryBuilder.bindMarker( "sequence_name" ) )
				.value( quote( metadata.getValueColumnName() ), QueryBuilder.bindMarker( "sequence_value" ) )
				.ifNotExists();

		PreparedStatement preparedStatement = provider.getSession().prepare( insert.toString() );
		BoundStatement boundStatement = preparedStatement.bind();
		boundStatement.setString( "sequence_name", sequenceName );
		boundStatement.setLong( "sequence_value", value );

		try {
			provider.getSession().execute( boundStatement );
		}
		catch (DriverException e) {
			throw e;
		}

		return nextValueSelect( metadata, sequenceName );
	}

	private boolean nextValueUpdate(IdSourceKeyMetadata metadata, String sequenceName, Long oldValue, Long newValue) {

		Statement update = provider.getQueryBuilder().update( quote( metadata.getName() ) )
				.with( set( quote( metadata.getValueColumnName() ), QueryBuilder.bindMarker( "sequence_value_new" ) ) )
				.where( eq( quote( metadata.getKeyColumnName() ), QueryBuilder.bindMarker( "sequence_name" ) ) )
				.onlyIf( eq( quote( metadata.getValueColumnName() ), QueryBuilder.bindMarker( "sequence_value_old" ) ) );

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
			throw e;
		}

		return resultSet.one().getBool( 0 );
	}

	public Number nextValue(NextValueRequest request) {

		IdSourceKey key = request.getKey();
		IdSourceKeyMetadata metadata = request.getKey().getMetadata();
		Long valueFromDb = null;
		boolean done = false;
		do {
			valueFromDb = nextValueSelect( metadata, key.getColumnValues()[0].toString() );

			if ( valueFromDb == null ) {
				//if not there, insert initial value
				valueFromDb = nextValueInsert( metadata,
						key.getColumnValues()[0].toString(),
						(long) request.getInitialValue()
				);
			}

			//update seq value ready for the next reader
			Long updatedValue = valueFromDb + (long) request.getIncrement();
			done = nextValueUpdate( metadata, key.getColumnValues()[0].toString(), valueFromDb, updatedValue );
		}
		while ( !done );

		return valueFromDb;
	}
}
