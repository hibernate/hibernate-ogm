/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.util.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.ogm.dialect.query.spi.ClosableIterator;

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

	public static <K, V> HashMap<K, V> newHashMap() {
		return new HashMap<K, V>();
	}

	public static <K, V> HashMap<K, V> newHashMap(int initialCapacity) {
		return new HashMap<K, V>( initialCapacity );
	}

	public static <K, V> HashMap<K, V> newHashMap(Map<? extends K, ? extends V> other) {
		return new HashMap<K, V>( other );
	}

	public static <K, V> ConcurrentHashMap<K, V> newConcurrentHashMap() {
		return new ConcurrentHashMap<K, V>();
	}

	public static <K, V> ConcurrentHashMap<K, V> newConcurrentHashMap(int initialCapacity) {
		return new ConcurrentHashMap<K, V>( initialCapacity );
	}

	public static <K, V> ConcurrentHashMap<K, V> newConcurrentHashMap(Map<? extends K, ? extends V> other) {
		return new ConcurrentHashMap<K, V>( other );
	}

	public static <T> ClosableIterator<T> newClosableIterator(Iterable<T> iterable) {
		return new ClosableIteratorWrapper<T>( iterable.iterator() );
	}

	public static boolean isEmptyOrContainsOnlyNull(Object[] objects) {
		for ( Object object : objects ) {
			if ( object != null ) {
				return false;
			}
		}
		return true;
	}

	private static class ClosableIteratorWrapper<T> implements ClosableIterator<T> {

		private final Iterator<T> iterator;

		private ClosableIteratorWrapper(Iterator<T> iterator) {
			this.iterator = iterator;
		}

		@Override
		public boolean hasNext() {
			return iterator.hasNext();
		}

		@Override
		public T next() {
			return iterator.next();
		}

		@Override
		public void remove() {
			iterator.remove();
		}

		@Override
		public void close() {
			//Nothing to do
		}
	}
}
