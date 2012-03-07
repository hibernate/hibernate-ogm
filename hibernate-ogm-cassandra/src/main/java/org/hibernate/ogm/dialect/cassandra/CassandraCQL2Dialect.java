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
import org.hibernate.ogm.datastore.cassandra.impl.CassandraDatastoreProvider;
import org.hibernate.ogm.datastore.impl.MapBasedTupleSnapshot;
import org.hibernate.ogm.datastore.spi.Association;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.datastore.spi.TupleOperation;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.dialect.cassandra.impl.ResultSetTupleSnapshot;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.util.impl.SerializationHelper;
import org.hibernate.persister.entity.Lockable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Uses CQL syntax v2
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class CassandraCQL2Dialect implements GridDialect {

	private final CassandraDatastoreProvider provider;

	public CassandraCQL2Dialect(CassandraDatastoreProvider provider) {
		this.provider = provider;
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
		String table = "GenericTable"; // FIXME with key.getTable();
		String idColumnName = "key"; //FIXME extract from key but not present today
		//NOTE: SELECT ''..'' returns all columns except the key
		StringBuilder query = new StringBuilder( "SELECT * " )
				.append( "FROM " ).append( table )
				.append( " WHERE " ).append( idColumnName )
				.append( "=?" );

		ResultSet resultSet;
		try {
			PreparedStatement statement = provider.getConnection().prepareStatement( query.toString() );
			statement.setBytes( 1, SerializationHelper.toByteArray( key.getId() ) );
			statement.execute( query.toString() );
			//FIXME close statement when done with resultset: Cassandra's driver is cool with that though
			statement.close();
			resultSet = statement.getResultSet();

		} catch ( SQLException e ) {
			throw new HibernateException( "Cannot execute select query in cassandra", e );
		}
		try {
			boolean next = resultSet.next();
			if ( next == false ) {
				return null;
			} else {
				return new Tuple( new ResultSetTupleSnapshot( resultSet ) );
			}
		} catch ( SQLException e ) {
			throw new HibernateException( "Error while reading resultset", e );
		}
	}

	@Override
	public Tuple createTuple(EntityKey key) {
		return new Tuple( new MapBasedTupleSnapshot( new HashMap<String, Object>() ) );
	}

	@Override
	public void updateTuple(Tuple tuple, EntityKey key) {
		String table = "GenericTable"; // FIXME with key.getTable();
		String idColumnName = "key"; //FIXME extract from key but not present today
		//NOTE: SELECT ''..'' returns all columns except the key
		StringBuilder query = new StringBuilder();
		query.append( "BEGIN BATCH;" );
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
			if ( updateOps.size() > 0 ) {
				query.append( "UPDATE " ).append( table ).append( " SET " )
						// column=?
						//TODO Finish this column=?
						.append( "WHERE " ).append( idColumnName )
						.append( "=?" );
			}
			if ( deleteOps.size() > 0 ) {
				query.append( "DELETE " )
						// column
						//TODO Finish this column
						.append( " FROM " ).append( table )
						.append( " WHERE " ).append( idColumnName )
						.append( "=?" );
			}
		}
		//TODO apply parameters

		//TODO execute query

		query.append( "APPLY BATCH;" );

	}

	@Override
	public void removeTuple(EntityKey key) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public Association getAssociation(AssociationKey key) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public Association createAssociation(AssociationKey key) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateAssociation(Association association, AssociationKey key) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void removeAssociation(AssociationKey key) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public Tuple createTupleAssociation(AssociationKey associationKey, RowKey rowKey) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void nextValue(RowKey key, IntegralDataTypeHolder value, int increment, int initialValue) {
		//To change body of implemented methods use File | Settings | File Templates.
	}
}
