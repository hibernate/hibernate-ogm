/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.mapping.mongodb;

import org.hibernate.ogm.datastore.mongodb.AssociationStorage;
import org.hibernate.ogm.mapping.context.EntityContext;
import org.hibernate.ogm.mapping.context.GlobalContext;
import org.hibernate.ogm.mapping.context.PropertyContext;
import org.hibernate.ogm.options.mongodb.AssociationStorageOption;
import org.hibernate.ogm.options.mongodb.WriteConcernOption;

import com.mongodb.WriteConcern;

/**
 * @author Davide D'Alto <davide@hibernate.org>
 */
public final class MongoDBMappingModel {

	public interface MongoDBGlobalOptions<T> {
		T writeConcern(WriteConcern concern);
	}

	public interface MongoDBEntityOptions<T> {
		T writeConcern(WriteConcern concern);
	}

	public interface MongoDBPropertyOptions<T> {
		T associationStorage(AssociationStorage concern);
	}

	public interface MongoDBMapping extends
			GlobalContext<MongoDBMapping, MongoDBEntityContext, MongoDBPropertyContext>,
			MongoDBGlobalOptions<MongoDBMapping> {
	}

	public interface MongoDBEntityContext extends
			EntityContext<MongoDBMapping, MongoDBEntityContext, MongoDBPropertyContext>,
			MongoDBEntityOptions<MongoDBEntityContext> {
	}

	public interface MongoDBPropertyContext extends
			PropertyContext<MongoDBMapping, MongoDBEntityContext, MongoDBPropertyContext>,
			MongoDBPropertyOptions<MongoDBPropertyContext> {
	}

	public static class MongoDBGlobalOptionsGenerator implements MongoDBGlobalOptions<Object> {
		@Override
		public WriteConcernOption writeConcern(WriteConcern writeConcern) {
			return new WriteConcernOption( writeConcern );
		}
	}

	public static class MongoDBEntityOptionsGenerator implements MongoDBEntityOptions<Object> {
		@Override
		public WriteConcernOption writeConcern(WriteConcern writeConcern) {
			return new WriteConcernOption( writeConcern );
		}
	}

	public static class MongoDBPropertyOptionsGenerator implements MongoDBPropertyOptions<Object> {
		@Override
		public AssociationStorageOption associationStorage(AssociationStorage storage) {
			return new AssociationStorageOption( storage );
		}
	}

}
