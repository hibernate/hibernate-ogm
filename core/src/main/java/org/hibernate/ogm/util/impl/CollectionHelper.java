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
package org.hibernate.ogm.util.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Provides commonly used functionality around collections.
 *
 * @author Gunnar Morling
 */
public class CollectionHelper {

	private CollectionHelper() {
	}

	/**
	 * Returns an unmodifiable set containing the given elements.
	 *
	 * @param ts the elements from which to create a set
	 * @return an unmodifiable set containing the given elements or {@code null} in case the given element array is
	 * {@code null}.
	 */
	public static <T> Set<T> asSet(T... ts) {
		if ( ts == null ) {
			return null;
		}
		else if ( ts.length == 0 ) {
			return Collections.emptySet();
		}
		else {
			Set<T> set = new HashSet<T>( ts.length );
			Collections.addAll( set, ts );
			return Collections.unmodifiableSet( set );
		}
	}
}
