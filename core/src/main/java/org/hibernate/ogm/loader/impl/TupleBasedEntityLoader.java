/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.loader.impl;

import java.util.List;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.entity.UniqueEntityLoader;

/**
 * "Loads" entities from given tuple representations.
 * <p>
 * TODO: This is used for the mass indexer and also to convert query results (tuple collections) into objects. At least
 * the latter shouldn't work that way, instead the loader itself should obtain the tuple result set from the datastore,
 * so we have query execution time statistics correct etc.
 *
 * @author Gunnar Morling
 */
public interface TupleBasedEntityLoader extends UniqueEntityLoader {

	/**
	 * Load a list of entities using the information in the context
	 *
	 * @param session The session
	 * @param lockOptions The locking details
	 * @param ogmContext The context with the information to load the entities
	 * @return the list of entities corresponding to the given context
	 */
	List<Object> loadEntitiesFromTuples(SharedSessionContractImplementor session, LockOptions lockOptions, OgmLoadingContext ogmContext);
}
