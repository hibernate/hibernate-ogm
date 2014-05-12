/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.datastore.neo4j.parser.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.engine.query.spi.ParameterMetadata;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.AbstractQueryImpl;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.MapsTupleIterator;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.NodesTupleIterator;
import org.hibernate.ogm.datastore.neo4j.impl.Neo4jDatastoreProvider;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.TupleIterator;
import org.hibernate.ogm.util.parser.impl.ObjectLoadingIterator;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;

/**
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class Neo4jQuery extends AbstractQueryImpl {

	private final Class<?> entityType;
	private final List<String> projections;
	private final ExecutionResult executeResult;

	public Neo4jQuery(Class<?> entityType, String query, List<String> projections, OgmSession session) {
		super( query, null, (SessionImplementor) session, new ParameterMetadata( null, null ) );
		Neo4jDatastoreProvider provider = neo4jProvider( (SessionImplementor) session );
		ExecutionEngine engine = new ExecutionEngine( provider.getDataBase() );
		this.entityType = entityType;
		this.projections = projections;
		this.executeResult = engine.execute( getQueryString() );
	}

	private static Neo4jDatastoreProvider neo4jProvider(SessionImplementor session) {
		return (Neo4jDatastoreProvider) session
				.getFactory()
				.getServiceRegistry()
				.getService( DatastoreProvider.class );
	}

	@Override
	public Query setLockOptions(LockOptions lockOptions) {
		return null;
	}

	@Override
	public Query setLockMode(String alias, LockMode lockMode) {
		return null;
	}

	@Override
	public Iterator<?> iterate() {
		TupleIterator iterator = null;
		if ( projections.isEmpty() ) {
			iterator = new NodesTupleIterator( executeResult );
		}
		else {
			iterator = new MapsTupleIterator( executeResult );
		}
		return new ObjectLoadingIterator( session, iterator, entityType, projections );
	}

	@Override
	public ScrollableResults scroll() {
		return null;
	}

	@Override
	public ScrollableResults scroll(ScrollMode scrollMode) {
		return null;
	}

	@Override
	public List<?> list() {
		ObjectLoadingIterator iterator = (ObjectLoadingIterator) iterate();
		try {
			List<Object> result = new ArrayList<Object>();
			while ( iterator.hasNext() ) {
				Object next = iterator.next();
				result.add( next );
			}
			return result;
		}
		finally {
			iterator.close();
		}

	}

	@Override
	public int executeUpdate() {
		return 0;
	}

	@Override
	public LockOptions getLockOptions() {
		return null;
	}

}
