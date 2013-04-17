/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.datastore.impl;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * Wrap a Collection object into a Set assuming the Collection does not contain duplicates.
 * The Set is read-only
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public final class SetFromCollection<E> implements Set<E> {
	private Collection delegate;

	public SetFromCollection(Collection delegate) {
		this.delegate = delegate;
	}

	@Override
	public int size() {
		return delegate.size();
	}

	@Override
	public boolean isEmpty() {
		return delegate.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return delegate.contains( o );
	}

	@Override
	public Iterator<E> iterator() {
		return delegate.iterator();
	}

	@Override
	public Object[] toArray() {
		return delegate.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return (T[]) delegate.toArray( a );
	}

	@Override
	public boolean add(E e) {
		throw new UnsupportedOperationException("This Set implementation is read-only");
	}

	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException("This Set implementation is read-only");
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return delegate.containsAll( c );
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		throw new UnsupportedOperationException("This Set implementation is read-only");
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return delegate.retainAll( c );
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException("This Set implementation is read-only");
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException("This Set implementation is read-only");
	}

}
