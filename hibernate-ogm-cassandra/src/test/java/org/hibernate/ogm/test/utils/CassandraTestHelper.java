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
package org.hibernate.ogm.test.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.datastax.driver.core.ColumnDefinitions;
import com.datastax.driver.core.DataType;
import com.datastax.driver.core.Query;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import com.datastax.driver.core.querybuilder.Clause;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.cfg.OgmConfiguration;
import org.hibernate.ogm.datastore.cassandra.impl.CassandraDatastoreProvider;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.exception.NotSupportedException;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.type.CassandraTypeConverter;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;

/**
 * @author Khanh Tuong Maudoux
 */
public class CassandraTestHelper implements TestableGridDialect {

	@Override
	public boolean assertNumberOfEntities(int numberOfEntities, SessionFactory sessionFactory) {
		CassandraDatastoreProvider provider = getProvider( sessionFactory );
		Session session = provider.getSession();

		Query query = select().from( "system", "schema_columns" ).where(
				eq(
						"keyspace_name",
						provider.getKeyspace()
				)
		);

		List<Row> result = null;
		try {
			result = session.execute( query ).all();
		}
		catch (NoHostAvailableException e) {
			throw new HibernateException( "Unable to execute a select query onto cassandra metadata table", e );
		}
		//TODO : it is supposed that associations have the character "_" into their columnName
		int numberOfEntitiesTmp = 0;
		Set<String> entitiesTable = new HashSet<String>(  );

		for ( Row row : result ) {
			String entityName = row.getString( "columnfamily_name" );
			if ( entityName != null && !entityName.contains( "_" ) ) {
				entitiesTable.add( entityName );
			}
		}

		//for each entity table, do a count(*) and add result
		for (String entityTableName : entitiesTable) {
			query = select().countAll().from(entityTableName);

			try {
				numberOfEntitiesTmp += session.execute( query ).one().getLong( "count" );
			}
			catch (NoHostAvailableException e) {
				throw new HibernateException( "Unable to count data into cassandra table", e );
			}
		}

		return numberOfEntities == numberOfEntitiesTmp;
	}

	@Override
	public boolean assertNumberOfAssociations(int numberOfAssociations, SessionFactory sessionFactory) {
		CassandraDatastoreProvider provider = getProvider( sessionFactory );
		Session session = provider.getSession();

		Query query = select(  ).from( "system", "schema_columns" ).where(
				eq(
						"keyspace_name",
						provider.getKeyspace()
				)
		);

		List<Row> result = null;
		try {
			result = session.execute( query ).all();
		}
		catch (NoHostAvailableException e) {
			throw new HibernateException( "Unable to execute a select query onto cassandra metadata table", e );
		}
		//TODO : it is supposed that associations have the character "_" into their columnName
		int numberOfEntitiesTmp = 0;

		Set<String> entitiesTable = new HashSet<String>(  );

		for ( Row row : result ) {
			String entityName = row.getString( "columnfamily_name" );
			if ( entityName != null && entityName.contains( "_" ) ) {
				entitiesTable.add( entityName );
			}
		}

		//for each entity table, do a count(*) and add result
		for (String entityTableName : entitiesTable) {
			query = select().countAll().from(entityTableName);

			try {
				numberOfEntitiesTmp += session.execute( query ).one().getLong( "count" );
			}
			catch (NoHostAvailableException e) {
				throw new HibernateException( "Unable to count data into cassandra table", e );
			}
		}

		return numberOfAssociations == numberOfEntitiesTmp;
	}

	@Override
	public Map<String, Object> extractEntityTuple(SessionFactory sessionFactory, EntityKey key) {
		CassandraDatastoreProvider provider = getProvider( sessionFactory );
		Session session = provider.getSession();

		//TODO : only for unique pk
		Query query = select(  ).from( key.getTable() ).where(
				eq(
						key.getColumnNames()[0],
						key.getColumnValues()[0]
				)
		);

		Row row = null;
		try {
			row = session.execute( query ).one();
		}
		catch (NoHostAvailableException e) {
			throw new HibernateException( "Unable to execute a select query in cassandra", e );
		}

		ColumnDefinitions columnDefinitions = row.getColumnDefinitions();

		Map<String, Object> result = new HashMap<String, Object>(  );

		for (ColumnDefinitions.Definition definition : columnDefinitions.asList()) {
			DataType type = definition.getType();
			Object value = CassandraTypeConverter.getValue( row, definition.getName(), type );
			result.put( definition.getName(), (value == null) ? null : value.toString() );
		}

		return result;
	}

	@Override
	public boolean backendSupportsTransactions() {
		return false;
	}

	@Override
	public void dropSchemaAndDatabase(SessionFactory sessionFactory) {
		CassandraDatastoreProvider provider = getProvider( sessionFactory );
		provider.forceDropSchema();
	}

	@Override
	public Map<String, String> getEnvironmentProperties() {
		return null;
	}

	public static CassandraDatastoreProvider getProvider(SessionFactory sessionFactory) {
		DatastoreProvider provider = ((SessionFactoryImplementor) sessionFactory).getServiceRegistry().getService(
				DatastoreProvider.class
		);
		if ( !(CassandraDatastoreProvider.class.isInstance( provider )) ) {
			throw new RuntimeException( "Not testing with Cassandra" );
		}
		return CassandraDatastoreProvider.class.cast( provider );
	}

}
