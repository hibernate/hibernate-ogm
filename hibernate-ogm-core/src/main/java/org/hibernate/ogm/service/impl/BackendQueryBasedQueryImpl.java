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
package org.hibernate.ogm.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.engine.query.spi.ParameterMetadata;
import org.hibernate.internal.AbstractQueryImpl;
import org.hibernate.ogm.hibernatecore.impl.OgmSession;
import org.hibernate.ogm.loader.OgmQueryLoader;

/**
 * A {@link Query} implementation which uses the native query capabilities of the chosen store by delegating query
 * execution to a {@link org.hibernate.ogm.datastore.spi.BackendQuery}, using a {@link OgmQueryLoader}.
 *
 * @author Gunnar Morling
 */
public class BackendQueryBasedQueryImpl extends AbstractQueryImpl {

	private final OgmQueryLoader queryLoader;

	public BackendQueryBasedQueryImpl(OgmSession session, OgmQueryLoader queryLoader) {
		super( queryLoader.getQueryString(), null, session, new ParameterMetadata( null, null ) );
		this.queryLoader = queryLoader;
	}

	@Override
	public Iterator<?> iterate() throws HibernateException {
		return queryLoader.execute( (OgmSession) session, Collections.<String, Object>emptyMap() );
	}

	@Override
	public ScrollableResults scroll() throws HibernateException {
		throw new UnsupportedOperationException( "Not implemented yet" );
	}

	@Override
	public ScrollableResults scroll(ScrollMode scrollMode) throws HibernateException {
		throw new UnsupportedOperationException( "Not implemented yet" );
	}

	@Override
	public List<?> list() throws HibernateException {
		Iterator<?> results = iterate();

		List<Object> result = new ArrayList<Object>();

		while ( results.hasNext() ) {
			result.add( results.next() );
		}

		return result;
	}

	@Override
	public int executeUpdate() throws HibernateException {
		throw new UnsupportedOperationException( "Not implemented yet" );
	}

	@Override
	public Query setLockOptions(LockOptions lockOptions) {
		throw new UnsupportedOperationException( "Not implemented yet" );
	}

	@Override
	public Query setLockMode(String alias, LockMode lockMode) {
		throw new UnsupportedOperationException( "Not implemented yet" );
	}

	@Override
	public LockOptions getLockOptions() {
		throw new UnsupportedOperationException( "Not implemented yet" );
	}
}
