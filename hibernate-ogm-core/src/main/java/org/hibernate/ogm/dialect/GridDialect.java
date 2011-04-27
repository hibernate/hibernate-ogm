/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2010-2011 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.dialect;

import java.util.Map;

import org.infinispan.Cache;

import org.hibernate.LockMode;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.persister.entity.Lockable;

/**
 * Dialect abstracting Hibernate OGM from the grid implementation
 *
 * @author Emmanuel Bernard
 */
public interface GridDialect {
	LockingStrategy getLockingStrategy(Lockable lockable, LockMode lockMode);

	/**
	 * Return the tuple for a given key in a given cache or null if not present
	 */
	Map<String,Object> getTuple(EntityKey key, Cache<EntityKey, Map<String, Object>> cache);
	/**
	 * Return a new tuple for a given key in a given cache
	 */
	Map<String,Object> createTuple(EntityKey key, Cache<EntityKey, Map<String, Object>> cache);
	/**
	 * Update the tuple for a given key in a given cache or null if not present
	 */
	void updateTuple(Map<String, Object> tuple, EntityKey key, Cache<EntityKey, Map<String, Object>> cache);

	/**
	 * Remove the tuple for a given key in a given cache
	 */
	void removeTuple(EntityKey key, Cache<EntityKey, Map<String, Object>> cache);
}
