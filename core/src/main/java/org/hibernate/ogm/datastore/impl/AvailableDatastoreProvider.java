/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
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

/**
 * This enumeration describes all available datastore providers by providing some shortcuts.
 * It's used for the Datastore Provider initialization to find the provider to instantiate.
 * @author Guillaume Scheibel <guillaume.scheibel@gmail.com>
 */
public enum AvailableDatastoreProvider {
	MAP(  "org.hibernate.ogm.datastore.map.impl.MapDatastoreProvider" ),
	INFINISPAN( "org.hibernate.ogm.datastore.infinispan.impl.InfinispanDatastoreProvider" ),
	EHCACHE( "org.hibernate.ogm.datastore.ehcache.impl.EhcacheDatastoreProvider" ),
	MONGODB( "org.hibernate.ogm.datastore.mongodb.impl.MongoDBDatastoreProvider" ),
	NEO4J_EMBEDDED( "org.hibernate.ogm.datastore.neo4j.impl.Neo4jDatastoreProvider" );

	private String datastoreProviderClassName;
	private AvailableDatastoreProvider(String datastoreProviderClassName) { this.datastoreProviderClassName = datastoreProviderClassName; }
	public String getDatastoreProviderClassName() { return this.datastoreProviderClassName; }
}
