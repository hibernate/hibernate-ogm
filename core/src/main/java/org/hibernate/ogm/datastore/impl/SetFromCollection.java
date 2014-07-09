/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.impl;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * Wrap a Collection object into a Set assuming the Collection does not contain duplicates.
 * The Set is read-only
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
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
