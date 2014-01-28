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
package org.hibernate.ogm.options.mongodb.mapping;

import org.hibernate.ogm.datastore.mongodb.AssociationDocumentType;
import org.hibernate.ogm.options.navigation.document.DocumentStorePropertyContext;

/**
 * Allows to configure MongoDB-specific options for a single property.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 * @author Gunnar Morling
 */
public interface MongoDBPropertyContext extends DocumentStorePropertyContext<MongoDBEntityContext, MongoDBPropertyContext> {

	/**
	 * Specifies how association documents should be persisted. Only applies when the association storage strategy is
	 * set to {@link org.hibernate.ogm.options.generic.document.AssociationStorageType#ASSOCIATION_DOCUMENT}.
	 *
	 * @param associationDocumentStorage the association document type to be used; overrides any settings on the entity
	 * or global level
	 * @return this context, allowing for further fluent API invocations
	 */
	MongoDBPropertyContext associationDocumentStorage(AssociationDocumentType associationDocumentStorage);
}
