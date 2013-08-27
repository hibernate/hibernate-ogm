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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.hibernate.engine.spi.TypedValue;

/**
 * Common base functionality for {@link QueryParserService} implementations.
 *
 * @author Gunnar Morling
 */
public abstract class BaseQueryParserService implements QueryParserService {

	/**
	 * Unwraps the given named parameters if they are wrapped into {@link TypedValue}s.
	 *
	 * @param namedParameters the original named parameters
	 * @return the unwrapped named parameters
	 */
	protected Map<String, Object> unwrap(Map<String, Object> namedParameters) {
		Map<String, Object> unwrapped = new HashMap<String, Object>( namedParameters.size() );

		for ( Entry<String, Object> entry : namedParameters.entrySet() ) {
			Object value = entry.getValue();
			unwrapped.put( entry.getKey(), value instanceof TypedValue ? ( (TypedValue) value ).getValue() : value );
		}

		return unwrapped;
	}
}
