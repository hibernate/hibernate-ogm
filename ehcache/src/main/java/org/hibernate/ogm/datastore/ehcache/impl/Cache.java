/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ehcache.impl;

import java.util.List;

import net.sf.ehcache.Element;

/**
 * Type-safe wrapper around {@link net.sf.ehcache.Cache} to avoid accessing the cache using wrong key objects.
 *
 * @author Gunnar Morling
 */
public class Cache<K> {

	private final net.sf.ehcache.Cache delegate;

	public Cache(net.sf.ehcache.Cache delegate) {
		this.delegate = delegate;
	}

	public Element get(K key) {
		return delegate.get( key );
	}

	public List<K> getKeys() {
		return delegate.getKeys();
	}

	public boolean remove(K key) {
		return delegate.remove( key );
	}

	public Element putIfAbsent(Element element) {
		return delegate.putIfAbsent( element );
	}

	public void put(Element element) {
		delegate.put( element );
	}

	public boolean replace(Element old, Element element) {
		return delegate.replace( old, element );
	}

	public int getSize() {
		return delegate.getSize();
	}
}
