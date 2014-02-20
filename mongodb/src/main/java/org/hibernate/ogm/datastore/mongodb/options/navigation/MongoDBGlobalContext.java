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
package org.hibernate.ogm.datastore.mongodb.options.navigation;

import org.hibernate.ogm.datastore.document.options.navigation.DocumentStoreGlobalContext;
import org.hibernate.ogm.datastore.mongodb.options.AssociationDocumentType;
import org.hibernate.ogm.datastore.mongodb.options.WriteConcernType;

/**
 * Allows to configure MongoDB-specific options applying on a global level. These options may be overridden for single
 * entities or properties.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 * @author Gunnar Morling
 */
public interface MongoDBGlobalContext extends DocumentStoreGlobalContext<MongoDBGlobalContext, MongoDBEntityContext> {

	/**
	 * Defines the type of write concern to be applied when performing write operations against the datastore.
	 *
	 * @param concern the write concern type
	 * @return this context, allowing for further fluent API invocations
	 * @see http://docs.mongodb.org/manual/core/write-concern/
	 */
	MongoDBGlobalContext writeConcern(WriteConcernType concern);

	/**
	 * Specifies how association documents should be persisted. Only applies when the association storage strategy is
	 * set to {@link org.hibernate.ogm.datastore.document.options.AssociationStorageType#ASSOCIATION_DOCUMENT}.
	 *
	 * @param associationDocumentStorage the association document type to be used when not configured on the entity or
	 * property level
	 * @return this context, allowing for further fluent API invocations
	 */
	MongoDBGlobalContext associationDocumentStorage(AssociationDocumentType associationDocumentStorage);
}
