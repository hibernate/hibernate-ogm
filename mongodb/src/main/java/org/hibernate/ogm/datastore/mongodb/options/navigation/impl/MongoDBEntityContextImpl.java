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
package org.hibernate.ogm.datastore.mongodb.options.navigation.impl;

import org.hibernate.ogm.datastore.document.options.navigation.impl.DocumentStoreEntityContextImpl;
import org.hibernate.ogm.datastore.mongodb.options.AssociationDocumentType;
import org.hibernate.ogm.datastore.mongodb.options.ReadPreferenceType;
import org.hibernate.ogm.datastore.mongodb.options.WriteConcernType;
import org.hibernate.ogm.datastore.mongodb.options.impl.AssociationDocumentStorageOption;
import org.hibernate.ogm.datastore.mongodb.options.impl.ReadPreferenceOption;
import org.hibernate.ogm.datastore.mongodb.options.impl.WriteConcernOption;
import org.hibernate.ogm.datastore.mongodb.options.navigation.MongoDBEntityContext;
import org.hibernate.ogm.datastore.mongodb.options.navigation.MongoDBPropertyContext;
import org.hibernate.ogm.options.navigation.impl.ConfigurationContext;
import org.hibernate.ogm.util.impl.Contracts;

import com.mongodb.WriteConcern;

/**
 * Converts MongoDB entity-level options.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 * @author Gunnar Morling
 */
public abstract class MongoDBEntityContextImpl extends DocumentStoreEntityContextImpl<MongoDBEntityContext, MongoDBPropertyContext> implements
		MongoDBEntityContext {

	public MongoDBEntityContextImpl(ConfigurationContext context) {
		super( context );
	}

	@Override
	public MongoDBEntityContext writeConcern(WriteConcernType writeConcern) {
		Contracts.assertParameterNotNull( writeConcern, "writeConcern" );
		addEntityOption( new WriteConcernOption(), writeConcern.getWriteConcern() );
		return this;
	}

	@Override
	public MongoDBEntityContext writeConcern(WriteConcern writeConcern) {
		Contracts.assertParameterNotNull( writeConcern, "writeConcern" );
		addEntityOption( new WriteConcernOption(), writeConcern );
		return this;
	}

	@Override
	public MongoDBEntityContext readPreference(ReadPreferenceType readPreference) {
		Contracts.assertParameterNotNull( readPreference, "readPreference" );
		addEntityOption( new ReadPreferenceOption(), readPreference.getReadPreference() );
		return this;
	}

	@Override
	public MongoDBEntityContext associationDocumentStorage(AssociationDocumentType associationDocumentStorage) {
		Contracts.assertParameterNotNull( associationDocumentStorage, "associationDocumentStorage" );
		addEntityOption( new AssociationDocumentStorageOption(), associationDocumentStorage );
		return this;
	}
}
