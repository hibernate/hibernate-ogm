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
package org.hibernate.ogm.datastore.spi;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.ogm.hibernatecore.impl.OgmSession;

/**
 * A store-specific representation of query. Can be executed several times using different sets of parameters.
 *
 * @author Gunnar Morling
 */
public interface BackendQuery {

	/**
	 * The entity type targeted by this query.
	 *
	 * @return the entity type targeted by this query
	 */
	Class<?> getEntityType();

	/**
	 * Whether this is a projection query or not.
	 *
	 * @return {@code true} in case this is a projection query, {@code false} otherwise.
	 */
	boolean isProjection();

	/**
	 * The names of the projected columns in case this is a projection query.
	 *
	 * @return the names of the projected columns in case this is a projection query, an empty list otherwise.
	 */
	List<String> getProjectionColumns();

	/**
	 * Executes this query via the given session, using the given named parameters.
	 *
	 * @param session the session to be used for query execution
	 * @param namedParameters named parameters for the query. Can be empty.
	 * @return an iterator of {@link Tuple}s representing the query result
	 */
	Iterator<Tuple> execute(OgmSession session, Map<String, Object> namedParameters);
}
