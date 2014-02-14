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
