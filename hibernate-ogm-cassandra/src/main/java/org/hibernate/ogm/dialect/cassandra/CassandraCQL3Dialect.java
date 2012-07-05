/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.dialect.cassandra;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.id.IntegralDataTypeHolder;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.ogm.datastore.cassandra.impl.CassandraDatastoreProvider;
import org.hibernate.ogm.datastore.cassandra.impl.CassandraTypeMapper;
import org.hibernate.ogm.datastore.impl.MapBasedTupleSnapshot;
import org.hibernate.ogm.datastore.mapbased.impl.MapAssociationSnapshot;
import org.hibernate.ogm.datastore.spi.Association;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.datastore.spi.TupleOperation;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.dialect.cassandra.impl.ResultSetAssociationSnapshot;
import org.hibernate.ogm.dialect.cassandra.impl.ResultSetTupleSnapshot;
import org.hibernate.ogm.exception.NotSupportedException;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.type.BigIntegerType;
import org.hibernate.ogm.type.CalendarType;
import org.hibernate.ogm.type.DateType;
import org.hibernate.ogm.type.GridType;
import org.hibernate.ogm.type.IntegerType;
import org.hibernate.ogm.type.StringBigDecimal;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Uses CQL syntax v3
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class CassandraCQL3Dialect implements GridDialect {

	private final CassandraDatastoreProvider provider;
	private final String keyspace;

	public CassandraCQL3Dialect(CassandraDatastoreProvider provider) {
		this.provider = provider;
		this.keyspace = provider.getKeyspace();
	}

	@Override
	public LockingStrategy getLockingStrategy(Lockable lockable, LockMode lockMode) {
		//Cassandra essentially has no workable lock strategy unless you use external tools like
		// ZooKeeper or any kind of lock keeper
		// FIXME find a way to reflect that in the implementation
		return null;
	}

	@Override
	public Tuple getTuple(EntityKey key) {
		String table = key.getTable();

		//TODO : fix me : ok just for simple pk
		String idColumnName = key.getColumnNames()[0];

		StringBuilder query = new StringBuilder( "SELECT * " )
				.append( "FROM " ).append( table )
				.append( " WHERE " ).append( idColumnName )
				.append( "=?" );

		ResultSet resultSet;
		boolean next;
		try {
			PreparedStatement statement = provider.getConnection().prepareStatement( query.toString() );
			statement.setString( 1, key.getColumnValues()[0].toString() );
			statement.execute();
			resultSet = statement.getResultSet();
			//FIXME close statement when done with resultset: Cassandra's driver is cool with that though
			statement.close();
		} catch ( SQLException e ) {
			throw new HibernateException( "Cannot execute select query in cassandra", e );
		}
		try {
			next = resultSet.next();
		}
		catch ( SQLException e ) {
			throw new HibernateException("Error while reading resultset", e);
		}
		if ( next == false ) {
			//FIXME Cassandra CQL/JDBC driver return a pseudo row even if the entity does not exists
			// see https://github.com/hibernate/hibernate-ogm/pull/50#issuecomment-4391896
			return null;
		} else {
			Tuple tuple = new Tuple( new ResultSetTupleSnapshot( resultSet, this.provider.getMetaDataCache().get( table )  ) );
			if (tuple.getColumnNames().size() == 1 ) {
				return null;
			}
			else {
				return tuple;
			}
		}
	}

	@Override
	public Tuple createTuple(EntityKey key) {
		Map<String, Object> toSave = new HashMap<String, Object>(  );
		toSave.put( key.getColumnNames()[0], key.getColumnValues()[0] );
		return new Tuple( new MapBasedTupleSnapshot( toSave ) );
	}

	@Override
	public void updateTuple(Tuple tuple, EntityKey key) {
		String table = key.getTable();

		//TODO : fix me : ok just for simple pk
		String idColumnName = key.getColumnNames()[0];
		StringBuilder query = new StringBuilder();

		List<TupleOperation> updateOps = new ArrayList<TupleOperation>( tuple.getOperations().size() );
		List<TupleOperation> deleteOps = new ArrayList<TupleOperation>( tuple.getOperations().size() );


		for ( TupleOperation op : tuple.getOperations() ) {
			switch ( op.getType() ) {
				case PUT:
						updateOps.add( op );
					break;
				case REMOVE:
				case PUT_NULL:
					deleteOps.add( op );
					break;
				default:
					throw new HibernateException( "TupleOperation not supported: " + op.getType() );
			}
			if ( deleteOps.size() > 0 ) {
				query.append( "DELETE " )
						// column
						//TODO Finish this column
						.append( " FROM " ).append( table )
						.append( " WHERE " ).append( idColumnName )
						.append( "=?;" );
			}
		}

		StringBuilder keyList = new StringBuilder();
		StringBuilder valueList = new StringBuilder();

		String prefix = "";
		//cassandra can not insert just the pk and need at least one column: http://cassandra.apache.org/doc/cql/CQL.html#INSERT
		boolean containsKey = false;
		for ( TupleOperation op : updateOps ) {
			keyList.append( prefix );
			valueList.append( prefix );
			prefix = ",";
			keyList.append( op.getColumn() );
			if (op.getColumn().equals( idColumnName )) {
				containsKey = true;
			}
			valueList.append( "'" ).append( op.getValue() ).append( "'" );
		}

		String queryQuestionMark = generateQueryQuestionMark( updateOps.size() );

		//check if keyList contains the key. If not, add it
		if (!containsKey) {
			keyList.append( "," ).append( idColumnName );
			queryQuestionMark += ",?";
		}

		//TODO: fixme: because it is not possible to insert a row without another column
		if (!keyList.toString().contains( "," )) {
			Table tableMetadata = provider.getMetaDataCache().get( table );
			Iterator columnIterator = tableMetadata.getColumnIterator();
			while (columnIterator.hasNext()) {
				Column column = (Column)columnIterator.next();
				if ( !keyList.toString().contains( column.getName() ) && !"dtype".equalsIgnoreCase( column.getName() ) ) {
					keyList.append( "," + column.getName() );
					queryQuestionMark += ", 'null'";
					break;
				}
			}
		}

		query.append( "INSERT INTO " )
				.append( table )
				.append( "(" )
				.append( keyList )
				.append( ") VALUES(" )
				.append( queryQuestionMark )
				.append( ");" );


		PreparedStatement statement = null;
		try {
			statement = provider.getConnection().prepareStatement( query.toString() );
			for (int i = 1 ; i <= updateOps.size() ; i ++) {

				TupleOperation tupleOperation = updateOps.get( i - 1 );
				String type = CassandraTypeMapper.INSTANCE.getType( this.provider, table, tupleOperation.getColumn() );

				if ("blob".equalsIgnoreCase( type )) {
					statement.setBytes( i, (byte[])updateOps.get( i - 1 ).getValue() );
				}
				else if ( "int".equalsIgnoreCase( type ) ) {
					statement.setInt( i, Integer.valueOf( tupleOperation.getValue().toString() ) );
				}
				else if ( "boolean".equalsIgnoreCase( type ) ) {
					statement.setBoolean( i, Boolean.valueOf( tupleOperation.getValue().toString() ) );
				}
				else if ( "float".equalsIgnoreCase( type ) ) {
					statement.setFloat( i, Float.valueOf( tupleOperation.getValue().toString() ) );
				}
				else {
					statement.setString( i, tupleOperation.getValue().toString() );
				}
			}
			if (!containsKey) {
				statement.setObject( updateOps.size() + 1, key.getColumnValues()[0] );
			}

			statement.execute();
			statement.close();
		}
		catch (SQLException e) {
			throw new HibernateException( "unable to insert values into " + key.getTable(), e );
		}

	}



	@Override
	public void removeTuple(EntityKey key) {
		String table = key.getTable();

		//TODO : fix me : ok just for simple pk
		String idColumnName = key.getColumnNames()[0];

		StringBuilder query = new StringBuilder( "DELETE " )
				.append( "FROM " ).append( table )
				.append( " WHERE " ).append( idColumnName )
				.append( "=?" );

		try {
			PreparedStatement statement = provider.getConnection().prepareStatement( query.toString() );
			statement.setString( 1, key.getColumnValues()[0].toString() );
			statement.execute();
			statement.close();
		} catch ( SQLException e ) {
			throw new HibernateException( "Cannot execute select query in cassandra", e );
		}
	}

	@Override
	public Association getAssociation(AssociationKey key) {
		String table = key.getTable();

		//TODO : fix me : ok just for simple pk
		String idColumnName = key.getColumnNames()[0];

		StringBuilder query = new StringBuilder( "SELECT * " )
				.append( "FROM " ).append( table )
				.append( " WHERE " ).append( idColumnName )
				.append( "=?" );

		ResultSet resultSet;
		boolean next;
		try {
			PreparedStatement statement = provider.getConnection().prepareStatement( query.toString() );
			statement.setString( 1, key.getColumnValues()[0].toString() );
			statement.execute();
			resultSet = statement.getResultSet();
			//FIXME close statement when done with resultset: Cassandra's driver is cool with that though
			statement.close();
		} catch ( SQLException e ) {
			throw new HibernateException( "Cannot execute select query in cassandra", e );
		}
		try {
			next = resultSet.next();
		}
		catch ( SQLException e ) {
			throw new HibernateException("Error while reading resultset", e);
		}
		if ( next == false ) {
			//FIXME Cassandra CQL/JDBC driver return a pseudo row even if the entity does not exists
			// see https://github.com/hibernate/hibernate-ogm/pull/50#issuecomment-4391896
			return null;
		} else {
			RowKey rowKey = null;
			if (key.getEntityKey() != null) {
				rowKey = new RowKey(
						key.getEntityKey().getTable(),
						key.getEntityKey().getColumnNames(),
						key.getEntityKey().getColumnValues()
				);
			} else {
				rowKey = new RowKey( key.getTable(), key.getColumnNames(), key.getColumnValues() );
			}
			Association association = new Association(
					new ResultSetAssociationSnapshot(
							rowKey,
							resultSet,
							provider.getMetaDataCache()
									.get( table )
					)
			);
			if (association.getKeys().size() == 1 ) {
				return association;
			}
			else {
				return association;
			}
		}
	}

	@Override
	public Association createAssociation(AssociationKey key) {
		Map<RowKey, Map<String, Object>> toSave = new HashMap<RowKey, Map<String, Object>>(  );
		RowKey rowKey = new RowKey( key.getTable(), key.getColumnNames(), key.getColumnValues() );
		toSave.put( rowKey, null );
		return new Association( new MapAssociationSnapshot( toSave ) );
	}

	@Override
	public void updateAssociation(Association association, AssociationKey key) {
		throw new NotSupportedException("OGM-122", "add association support");
	}

	private String generateQueryQuestionMark(int nbVariable) {
		StringBuilder sb = new StringBuilder(  );
		String prefix = "";
		for (int i = 0; i < nbVariable; i++) {
			sb.append( prefix );
			prefix = ",";
			sb.append( "?" );
		}
		return sb.toString();
	}

	@Override
	public void removeAssociation(AssociationKey key) {
		throw new NotSupportedException("OGM-122", "add association support");
	}

	@Override
	public Tuple createTupleAssociation(AssociationKey associationKey, RowKey rowKey) {
		Map<String, Object> toSave = new HashMap<String, Object>() ;
		toSave.put( rowKey.getColumnValues()[0].toString(), null );
		return new Tuple( new MapBasedTupleSnapshot( toSave ) );
	}

	@Override
	public void nextValue(RowKey key, IntegralDataTypeHolder value, int increment, int initialValue) {
	}

	@Override
	public GridType overrideType(Type type) {
		if (type == StandardBasicTypes.INTEGER ) {
			return IntegerType.INSTANCE;
		}
		if (type == StandardBasicTypes.BIG_INTEGER ) {
			return BigIntegerType.INSTANCE;
		}
		if (type == StandardBasicTypes.BIG_DECIMAL) {
			return StringBigDecimal.INSTANCE;
		}
		if (type == StandardBasicTypes.CALENDAR_DATE || type == StandardBasicTypes.CALENDAR) {
			return CalendarType.INSTANCE;
		}
		if (type == StandardBasicTypes.TIMESTAMP) {
			return DateType.INSTANCE;
		}
		if (type == StandardBasicTypes.TIME) {
			return DateType.INSTANCE;
		}
		if (type == StandardBasicTypes.DATE) {
			return DateType.INSTANCE;
		}
		//TODO : fix me
//		if (type.getName().equals( "org.hibernate.type.EnumType" ) ) {
//		}
		return null;
	}
}
