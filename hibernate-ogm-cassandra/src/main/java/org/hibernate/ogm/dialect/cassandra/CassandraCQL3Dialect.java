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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import com.datastax.driver.core.querybuilder.Batch;
import com.datastax.driver.core.querybuilder.Clause;
import com.datastax.driver.core.querybuilder.Delete;
import com.datastax.driver.core.querybuilder.Select;
import org.apache.cassandra.cql.CQLStatement;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.id.IntegralDataTypeHolder;
import org.hibernate.ogm.datastore.cassandra.impl.CassandraDatastoreProvider;
import org.hibernate.ogm.datastore.impl.MapTupleSnapshot;
import org.hibernate.ogm.datastore.map.impl.MapAssociationSnapshot;
import org.hibernate.ogm.datastore.spi.Association;
import org.hibernate.ogm.datastore.spi.AssociationContext;
import org.hibernate.ogm.datastore.spi.AssociationOperation;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.datastore.spi.TupleContext;
import org.hibernate.ogm.datastore.spi.TupleOperation;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.dialect.cassandra.impl.CassandraAssociationSnapshot;
import org.hibernate.ogm.dialect.cassandra.impl.CassandraTupleSnapshot;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.type.CassandraTypeConverter;
import org.hibernate.ogm.type.GridType;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.type.Type;

import static com.datastax.driver.core.querybuilder.QueryBuilder.batch;
import static com.datastax.driver.core.querybuilder.QueryBuilder.delete;
import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;

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
	public Tuple getTuple(EntityKey key, TupleContext tupleContext) {
		String table = key.getTable();

		//TODO : fix me : ok just for simple pk
		String idColumnName = key.getColumnNames()[0];

		Statement query = select( ).from( table ).where(
				eq(
						idColumnName,
						key.getColumnValues()[0].toString()
				)
		);

		List<Row> result = null;
		try {
			result = this.provider.getSession().execute( query ).all();
		}
		catch (NoHostAvailableException e) {
			throw new HibernateException( "Unable to execute select query", e );

		}
		if ( result.size() == 0 ) {
			return null;
		}
		else {
			return new Tuple( new CassandraTupleSnapshot( result, key ) );
		}
	}

	@Override
	public Tuple createTuple(EntityKey key) {
		Map<String, Object> toSave = new HashMap<String, Object>();
		toSave.put( key.getColumnNames()[0], key.getColumnValues()[0] );
		return new Tuple( new MapTupleSnapshot( toSave ) );
	}

	@Override
	public void updateTuple(Tuple tuple, EntityKey key) {
		String table = key.getTable();

		CQLStatement query = null;

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
		}

		List<Statement> queries = new ArrayList<Statement>();

		boolean needToInsertPrimaryKey = true;

		for ( TupleOperation op : updateOps ) {
			if ( !key.getColumnNames()[0].equals( op.getColumn() ) ) {
				String columnName = op.getColumn();
				if (op.getColumn().contains( "." )) {
					columnName = columnName.replaceAll( "\\.", "_" );
				}

				queries.add(
						insertInto(table).values( new String[] {key.getColumnNames()[0], columnName}, new Object[] {key.getColumnValues()[0], op.getValue()} )
				);
				needToInsertPrimaryKey = false;
			}
			else {
				needToInsertPrimaryKey &= true;
			}
		}

		for ( TupleOperation del : deleteOps ) {
			String columnName = del.getColumn();
			if (columnName.contains( "." )) {
				columnName = columnName.replaceAll( "\\.", "_" );
			}
			queries.add(
					delete( columnName ).from( table ).where(
							eq(
									key.getColumnNames()[0],
									key.getColumnValues()[0]
							)
					)
			);
		}

		if ( queries.isEmpty() || needToInsertPrimaryKey ) {
			//TODO try to insert just the key
			//TODO: cassandra can not insert just the pk and need at least one column: http://cassandra.apache.org/doc/cql/CQL.html#INSERT
			TupleOperation op = updateOps.get( 0 );
			queries.add(
					insertInto(table).values( new String[] {key.getColumnNames()[0], "dtype"}, new Object[] { key.getColumnValues()[0], op.getValue()} )
			);
		}
		Batch batch = batch( queries.toArray( new Statement[0] ) );
		try {
			this.provider.getSession().execute( batch );
		}
		catch (NoHostAvailableException e) {
			throw new HibernateException( "unable to insert values into " + key.getTable(), e );
		}

	}


	@Override
	public void removeTuple(EntityKey key) {
		String table = key.getTable();

		//TODO : fix me : ok just for simple pk
		Statement query = delete(  ).from( table ).where( eq( key.getColumnNames()[0], key.getColumnValues()[0] ) );

		try {
			this.provider.getSession().execute( query );
		}
		catch (NoHostAvailableException e) {
			throw new HibernateException( "Cannot execute select query in cassandra", e );
		}
	}

	@Override
	public Association getAssociation(AssociationKey key, AssociationContext associationContext) {
		String table = key.getTable();

		//TODO : fix me : ok just for simple pk
		String idColumnName = key.getColumnNames()[0];

		Statement query = select(  ).from( table ).where( eq( idColumnName, key.getColumnValues()[0].toString() ) );

		List<Row> result = null;
		try {
			result = this.provider.getSession().execute( query ).all();
		}
		catch (NoHostAvailableException e) {
			throw new HibernateException( "Cannot execute select query in cassandra", e );
		}
		if ( result.size() == 0 ) {
			return null;
		}
		else {
			RowKey rowKey = null;
			if ( key.getEntityKey() != null ) {
				rowKey = new RowKey(
						key.getEntityKey().getTable(),
						key.getEntityKey().getColumnNames(),
						key.getEntityKey().getColumnValues()
				);
			}
			else {
				rowKey = new RowKey( key.getTable(), key.getColumnNames(), key.getColumnValues() );
			}

			return new Association( new CassandraAssociationSnapshot( rowKey, result, key ) );
		}
	}

	@Override
	public Association createAssociation(AssociationKey key) {
		Map<RowKey, Map<String, Object>> toSave = new HashMap<RowKey, Map<String, Object>>();
		RowKey rowKey = new RowKey( key.getTable(), key.getColumnNames(), key.getColumnValues() );
		toSave.put( rowKey, null );
		return new Association( new MapAssociationSnapshot( toSave ) );
	}

	@Override
	public void updateAssociation(Association association, AssociationKey key) {

		String table = key.getTable();

		List<AssociationOperation> updateOps = new ArrayList<AssociationOperation>(
				association.getOperations()
						.size()
		);
		List<AssociationOperation> deleteOps = new ArrayList<AssociationOperation>(
				association.getOperations()
						.size()
		);

		for ( AssociationOperation op : association.getOperations() ) {
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
		}

		//TODO : cassandra can not insert just the pk and need at least one column: http://cassandra.apache.org/doc/cql/CQL.html#INSERT
		List<Statement> queries = new ArrayList<Statement>();

		for ( AssociationOperation op : updateOps ) {
			List<String> columnsName = new ArrayList<String>();
			columnsName.add( "id_" );
			Collections.addAll( columnsName, op.getKey().getColumnNames() );

			List<Object> columnsValue = new ArrayList<Object>();
			columnsValue.add( key.getColumnValues()[0].toString() );
			Collections.addAll( columnsValue, op.getKey().getColumnValues() );
			queries.add(
					insertInto(table).values( columnsName.toArray( new String[0] ), columnsValue.toArray( new Object[0] ) )
			);
		}

		for ( AssociationOperation del : deleteOps ) {
			queries.add(
					delete().from( table ).where(
							eq(
									"id_",
									key.getColumnValues()[0]
							)
					)
			);
		}

		Batch batch = batch( queries.toArray( new Statement[0] ) );
		try {
			this.provider.getSession().execute( batch );
		}
		catch (NoHostAvailableException e) {
			e.printStackTrace();
			throw new HibernateException( "unable to insert values into " + key.getTable(), e );
		}


	}

	@Override
	public void removeAssociation(AssociationKey key) {
		String table = key.getTable();

		//TODO : fix me : ok just for simple pk
		Statement query = delete(  ).from( table ).where( eq( "id_", key.getColumnValues()[0] ) );

		try {
			this.provider.getSession().execute( query );
		}
		catch (NoHostAvailableException e) {
			throw new HibernateException( "Cannot execute select query in cassandra", e );
		}
	}

	@Override
	public Tuple createTupleAssociation(AssociationKey associationKey, RowKey rowKey) {
		Map<String, Object> toSave = new HashMap<String, Object>();
		toSave.put( rowKey.getColumnValues()[0].toString(), null );
		return new Tuple( new MapTupleSnapshot( toSave ) );
	}

	@Override
	public void nextValue(RowKey key, IntegralDataTypeHolder value, int increment, int initialValue) {
	}

	@Override
	public GridType overrideType(Type type) {
		return CassandraTypeConverter.INSTANCE.convert( type );
	}
}
