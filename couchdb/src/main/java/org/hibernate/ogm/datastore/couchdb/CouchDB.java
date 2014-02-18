/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013-2014 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.datastore.couchdb;

import org.hibernate.ogm.datastore.couchdb.options.context.CouchDBGlobalContext;
import org.hibernate.ogm.datastore.couchdb.options.context.impl.CouchDBEntityContextImpl;
import org.hibernate.ogm.datastore.couchdb.options.context.impl.CouchDBGlobalContextImpl;
import org.hibernate.ogm.datastore.couchdb.options.context.impl.CouchDBPropertyContextImpl;
import org.hibernate.ogm.datastore.spi.DatastoreConfiguration;
import org.hibernate.ogm.options.navigation.impl.ConfigurationContext;

/**
 * Allows to configure options specific to the CouchDB document data store.
 *
 * @author Gunnar Morling
 * @author Andrea Boriero <dreborier@gmail.com/>
 */
public class CouchDB implements DatastoreConfiguration<CouchDBGlobalContext> {

	@Override
	public CouchDBGlobalContext getConfigurationBuilder(ConfigurationContext context) {
		return context.createGlobalContext( CouchDBGlobalContextImpl.class, CouchDBEntityContextImpl.class, CouchDBPropertyContextImpl.class );
	}
}
