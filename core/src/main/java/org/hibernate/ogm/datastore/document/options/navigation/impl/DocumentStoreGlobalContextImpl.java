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
package org.hibernate.ogm.datastore.document.options.navigation.impl;

import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.document.options.impl.AssociationStorageOption;
import org.hibernate.ogm.datastore.document.options.navigation.DocumentStoreEntityContext;
import org.hibernate.ogm.datastore.document.options.navigation.DocumentStoreGlobalContext;
import org.hibernate.ogm.options.navigation.impl.BaseGlobalContext;
import org.hibernate.ogm.options.navigation.impl.ConfigurationContext;

/**
 * Converts global document store options.
 *
 * @author Gunnar Morling
 */
public abstract class DocumentStoreGlobalContextImpl<G extends DocumentStoreGlobalContext<G, E>, E extends DocumentStoreEntityContext<E, ?>> extends
		BaseGlobalContext<G, E> implements DocumentStoreGlobalContext<G, E> {

	public DocumentStoreGlobalContextImpl(ConfigurationContext context) {
		super( context );
	}

	@Override
	public G associationStorage(AssociationStorageType associationStorage) {
		addGlobalOption( new AssociationStorageOption(), associationStorage );

		// ok; an error would only occur for inconsistently defined context types
		@SuppressWarnings("unchecked")
		G context = (G) this;
		return context;
	}
}
