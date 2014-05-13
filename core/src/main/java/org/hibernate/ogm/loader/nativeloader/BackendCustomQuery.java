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
package org.hibernate.ogm.loader.nativeloader;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.engine.query.spi.sql.NativeSQLQuerySpecification;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.custom.CustomQuery;
import org.hibernate.loader.custom.Return;
import org.hibernate.loader.custom.sql.SQLQueryReturnProcessor;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;

/**
 * Extension point allowing any NoSQL native query with named and positional parameters
 * to be executed by OGM on the corresponding backend, returning managed entities, collections and
 * simple scalar values.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class BackendCustomQuery implements CustomQuery {

	private static final Log LOG = LoggerFactory.make();

	private final NativeSQLQuerySpecification spec;
	private final Set<String> querySpaces;
	private final List<Return> customQueryReturns;

	public BackendCustomQuery(NativeSQLQuerySpecification spec, SessionFactoryImplementor factory) throws HibernateException {
		LOG.tracev( "Starting processing of NoSQL query [{0}]", spec.getQueryString() );

		this.spec = spec;

		SQLQueryReturnProcessor processor = new SQLQueryReturnProcessor(spec.getQueryReturns(), factory);
		processor.process();
		customQueryReturns = Collections.unmodifiableList( processor.generateCustomReturns( false ) );

		if ( spec.getQuerySpaces() != null ) {
			@SuppressWarnings("unchecked")
			Set<String> spaces = spec.getQuerySpaces();
			querySpaces = Collections.<String>unmodifiableSet( spaces );
		}
		else {
			querySpaces = Collections.emptySet();
		}
	}

	@Override
	public String getSQL() {
		return spec.getQueryString();
	}

	@Override
	public Set<String> getQuerySpaces() {
		return querySpaces;
	}

	@Override
	public Map<?, ?> getNamedParameterBindPoints() {
		// TODO: Should this actually be something more sensible?
		return Collections.emptyMap();
	}

	@Override
	public List<Return> getCustomQueryReturns() {
		return customQueryReturns;
	}

	public NativeSQLQuerySpecification getSpec() {
		return spec;
	}
}
