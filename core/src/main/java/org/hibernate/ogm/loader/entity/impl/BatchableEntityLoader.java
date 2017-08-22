/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.loader.entity.impl;

import java.io.Serializable;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.entity.UniqueEntityLoader;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;

/**
 * TODO: move that method to UniqueEntityLoader
 *
 * Contract between an entity loader and the batch fetching contract of BatchingEntityLoaderBuilder.
 *
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public interface BatchableEntityLoader extends UniqueEntityLoader {

	/**
	 * Called by wrappers that batch load entities
	 * @param persister only needed for logging
	 * @param lockOptions
	 */
	List<?> loadEntityBatch(
			final SharedSessionContractImplementor session,
			final Serializable[] ids,
			final Type idType,
			final Object optionalObject,
			final String optionalEntityName,
			final Serializable optionalId,
			final EntityPersister persister,
			LockOptions lockOptions) throws HibernateException;
}
