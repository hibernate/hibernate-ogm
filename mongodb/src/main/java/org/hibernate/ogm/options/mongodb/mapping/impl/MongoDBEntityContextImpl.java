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
package org.hibernate.ogm.options.mongodb.mapping.impl;

import org.hibernate.ogm.datastore.mongodb.AssociationDocumentType;
import org.hibernate.ogm.datastore.mongodb.WriteConcernType;
import org.hibernate.ogm.options.mongodb.impl.AssociationDocumentStorageOption;
import org.hibernate.ogm.options.mongodb.impl.WriteConcernOption;
import org.hibernate.ogm.options.mongodb.mapping.MongoDBEntityContext;
import org.hibernate.ogm.options.mongodb.mapping.MongoDBPropertyContext;
import org.hibernate.ogm.options.navigation.impl.ConfigurationContext;
import org.hibernate.ogm.options.navigation.impl.document.DocumentStoreEntityContextImpl;

/**
 * @author Davide D'Alto <davide@hibernate.org>
 * @author Gunnar Morling
 */
public abstract class MongoDBEntityContextImpl extends DocumentStoreEntityContextImpl<MongoDBEntityContext, MongoDBPropertyContext> implements
		MongoDBEntityContext {

	public MongoDBEntityContextImpl(ConfigurationContext context) {
		super( context );
	}

	@Override
	public MongoDBEntityContext writeConcern(WriteConcernType concern) {
		addEntityOption( new WriteConcernOption(), concern );
		return this;
	}

	@Override
	public MongoDBEntityContext associationDocumentStorage(AssociationDocumentType associationDocumentStorage) {
		addEntityOption( new AssociationDocumentStorageOption(), associationDocumentStorage );
		return this;
	}
}
