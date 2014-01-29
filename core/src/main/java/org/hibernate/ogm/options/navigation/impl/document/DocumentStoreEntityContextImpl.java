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
package org.hibernate.ogm.options.navigation.impl.document;

import org.hibernate.ogm.options.generic.document.AssociationStorageType;
import org.hibernate.ogm.options.generic.document.impl.AssociationStorageOption;
import org.hibernate.ogm.options.navigation.document.DocumentStoreEntityContext;
import org.hibernate.ogm.options.navigation.document.DocumentStorePropertyContext;
import org.hibernate.ogm.options.navigation.impl.BaseEntityContext;
import org.hibernate.ogm.options.navigation.impl.ConfigurationContext;

/**
 * Converts document store entity-level options.
 *
 * @author Gunnar Morling
 */
public abstract class DocumentStoreEntityContextImpl<E extends DocumentStoreEntityContext<E, P>, P extends DocumentStorePropertyContext<E, P>> extends
		BaseEntityContext<E> implements DocumentStoreEntityContext<E, P> {

	public DocumentStoreEntityContextImpl(ConfigurationContext context) {
		super( context );
	}

	@Override
	public E associationStorage(AssociationStorageType associationStorage) {
		addEntityOption( new AssociationStorageOption(), associationStorage );

		// ok; an error would only occur for inconsistently defined context types
		@SuppressWarnings("unchecked")
		E context = (E) this;
		return context;
	}
}
