/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013-2014 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.util.parser.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.dialect.TupleIterator;
import org.hibernate.ogm.loader.OgmLoader;
import org.hibernate.ogm.loader.OgmLoadingContext;
import org.hibernate.ogm.persister.OgmEntityPersister;

/**
 * @author Davide D'Alto
 */
public class ObjectLoadingIterator implements Iterator<Object> {

	private final TupleIterator tupleIterator;
	private final Collection<String> projections;
	private final SessionImplementor session;
	private final Class<?> entityType;

	public ObjectLoadingIterator(SessionImplementor session, TupleIterator tupleIterator, Class<?> entityType, Collection<String> projections) {
		this.entityType = entityType;
		this.session = session;
		this.tupleIterator = tupleIterator;
		this.projections = projections;
	}

	@Override
	public boolean hasNext() {
		return tupleIterator.hasNext();
	}

	@Override
	public Object next() {
		Tuple next = tupleIterator.next();
		if ( projections.isEmpty() ) {
			return getAsManagedEntity( next );
		}
		else {
			return getAsProjection( next );
		}
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException( "Not implemented yet" );
	}

	public void close() {
		tupleIterator.close();
	}

	private Object getAsManagedEntity(Tuple tuple) {
		OgmEntityPersister persister = (OgmEntityPersister) ( session.getFactory() ).getEntityPersister( entityType.getName() );
		OgmLoader loader = new OgmLoader( new OgmEntityPersister[] { persister } );
		OgmLoadingContext ogmLoadingContext = new OgmLoadingContext();
		ogmLoadingContext.setTuples( Arrays.asList( tuple ) );

		return loader.loadEntities( session, LockOptions.NONE, ogmLoadingContext ).iterator().next();
	}

	private Object[] getAsProjection(Tuple tuple) {
		Object[] projectionResult = new Object[projections.size()];
		int i = 0;

		for ( String column : projections ) {
			projectionResult[i] = tuple.get( column );
			i++;
		}

		return projectionResult;
	}

}
