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
package org.hibernate.ogm.cfg;

/**
 * Common properties for configuring document datastores such as MongoDB or CouchDB via {@code persistence.xml} or
 * {@link org.hibernate.ogm.cfg.OgmConfiguration}.
 * <p>
 * Note that not all properties are supported by all datastores; refer to the documentation of the specific dialect to
 * find out the supported configuration properties.
 *
 * @author Gunnar Morling
 */
public final class DocumentStoreProperties {

	/**
	 * Property for configuring the strategy for storing associations. Valid values are the
	 * {@link org.hibernate.ogm.options.couchdb.AssociationStorageType} enumeration and the String representation of its
	 * constants. Defaults to the in-entity storage strategy.
	 * <p>
	 * Note that any value specified via this property will be overridden by values configured via annotations or the
	 * programmatic API.
	 */
	public static final String ASSOCIATIONS_STORE = "hibernate.ogm.datastore.document.associations.store";

	private DocumentStoreProperties() {
	}
}
