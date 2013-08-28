/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.loader;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.datastore.spi.BackendQuery;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.hibernatecore.impl.OgmSession;
import org.hibernate.ogm.persister.OgmEntityPersister;

/**
 * Retrieves managed entities by executing a store-specific query and loading the returned tuples into objects.
 *
 * @author Gunnar Morling
 */
public class OgmQueryLoader {

	private final BackendQuery query;
	private final OgmLoader ogmLoader;

	public OgmQueryLoader(SessionFactoryImplementor sessionFactory, BackendQuery query) {
		this.query = query;

		OgmEntityPersister persister = (OgmEntityPersister) sessionFactory.getEntityPersister( query.getEntityType().getName() );
		this.ogmLoader = new OgmLoader( new OgmEntityPersister[] { persister } );
	}

	/**
	 * Executes the underlying backend query via the given session, using the given named parameters.
	 *
	 * @param session the session to execute the query through
	 * @param parameters named parameters, if any; Can be empty.
	 * @return an iterator with the results returned by the query
	 */
	public Iterator<Object> execute(OgmSession session, Map<String, Object> parameters) {
		return new ObjectLoadingIterator( session, query.execute( session, parameters ) );
	}

	/**
	 * Wraps an iterator of {@link Tuple}s, loading the contained objects while iterating.
	 *
	 * @author Gunnar Morling
	 */
	private class ObjectLoadingIterator implements Iterator<Object> {

		private final OgmSession session;
		private final Iterator<Tuple> resultIterator;

		public ObjectLoadingIterator(OgmSession session, Iterator<Tuple> resultIterator) {
			this.session = session;
			this.resultIterator = resultIterator;
		}

		@Override
		public boolean hasNext() {
			return resultIterator.hasNext();
		}

		@Override
		public Object next() {
			Tuple next = resultIterator.next();

			if ( query.isProjection() ) {
				return getAsProjection( next );
			}
			else {
				return getAsManagedEntity( next );
			}
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException( "Not implemented yet" );
		}

		private Object getAsManagedEntity(Tuple tuple) {
			OgmLoadingContext ogmLoadingContext = new OgmLoadingContext();
			ogmLoadingContext.setTuples( Arrays.asList( tuple ) );

			return ogmLoader.loadEntities( session, LockOptions.NONE, ogmLoadingContext ).iterator().next();
		}

		private Object[] getAsProjection(Tuple tuple) {
			Object[] projectionResult = new Object[query.getProjectionColumns().size()];
			int i = 0;

			for ( String column : query.getProjectionColumns() ) {
				projectionResult[i] = tuple.get( column );
				i++;
			}

			return projectionResult;
		}
	}

	public String getQueryString() {
		return query.toString();
	}
}
