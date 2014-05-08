/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2010-2014 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.dialect;

import org.hibernate.LockMode;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.id.IntegralDataTypeHolder;
import org.hibernate.ogm.datastore.spi.Association;
import org.hibernate.ogm.datastore.spi.AssociationContext;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.datastore.spi.TupleContext;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.EntityKeyMetadata;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.loader.nativeloader.BackendCustomQuery;
import org.hibernate.ogm.massindex.batchindexing.Consumer;
import org.hibernate.ogm.query.spi.ParameterMetadataBuilder;
import org.hibernate.ogm.type.GridType;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.service.Service;
import org.hibernate.type.Type;

/**
 * Dialect abstracting Hibernate OGM from the grid implementation
 *
 * @author Emmanuel Bernard
 * @author Gunnar Morling
 */
public interface GridDialect extends Service {

	LockingStrategy getLockingStrategy(Lockable lockable, LockMode lockMode);

	/**
	 * Return the tuple with the given column for a given key
	 */
	Tuple getTuple(EntityKey key, TupleContext tupleContext);

	/**
	 * Return a new tuple for a given key
	 * Only used if the tuple is not present
	 */
	Tuple createTuple(EntityKey key, TupleContext tupleContext);

	/**
	 * Update the tuple for a given key or null if not present
	 */
	void updateTuple(Tuple tuple, EntityKey key, TupleContext tupleContext);

	/**
	 * Remove the tuple for a given key
	 */
	void removeTuple(EntityKey key, TupleContext tupleContext);

	/**
	 * Return the list of tuples corresponding to a given association and the given context
	 */
	Association getAssociation(AssociationKey key, AssociationContext associationContext);

	/**
	 * Create an empty container for the list of tuples corresponding to a given association
	 * Only used if the association data is not present
	 */
	Association createAssociation(AssociationKey key, AssociationContext associationContext);

	/**
	 * Update a given list of tuples corresponding to a given association
	 */
	void updateAssociation(Association association, AssociationKey key, AssociationContext associationContext);

	/**
	 * Remove the list of tuples corresponding to a given association
	 */
	void removeAssociation(AssociationKey key, AssociationContext associationContext);

	Tuple createTupleAssociation(AssociationKey associationKey, RowKey rowKey);

	/**
	 * Whether the given association is stored within an entity structure or not. E.g. dialects for document stores may
	 * support storing associations within entity documents and would have to return {@code true} if this is the case
	 * for a given association.
	 *
	 * @param associationKey identifies the association of interest
	 * @return {@code true} if the specified association is stored within an entity structure, {@code false} otherwise.
	 */
	boolean isStoredInEntityStructure(AssociationKey associationKey, AssociationContext associationContext);

	/**
	 * Update value with the guaranteed next value with the defined increment
	 *
	 * Especially experimental
	 */
	void nextValue(RowKey key, IntegralDataTypeHolder value, int increment, int initialValue);

	/**
	 * Let the dialect override types if required to customize them to the datastore.
	 * Returns the GridType instance to use to bind the given {@code type} or null if not overridden.
	 *
	 * Most types should not be overridden and thus return null
	 *
	 * Experimental: this API might change in the future
	 */
	GridType overrideType(Type type);

	/**
	 * A consumer is called for each tuple matching the selected {@link EntityKeyMetadata}.
	 *
	 * @param consumer
	 *            the instance that is going to be called for every {@link Tuple}
	 * @param entityKeyMetadatas
	 *            the key metadata of the tables for which we want to apply the costumer
	 */
	void forEachTuple(Consumer consumer, EntityKeyMetadata... entityKeyMetadatas);

	/**
	 * Returns the result of a native query executed on the backend.
	 *
	 * @param customQuery the query to execute on the backend
	 * @param metadatas the metadata information of the results of the query
	 * @return an {@link Iterator} throught the result of the query
	 */
	TupleIterator executeBackendQuery(BackendCustomQuery customQuery, QueryParameters queryParameters, EntityKeyMetadata[] metadatas);

	ParameterMetadataBuilder getParameterMetadataBuilder();
}
