/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.identity.spi;

import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.spi.Tuple;

/**
 * A {@link GridDialect} facet to be implemented by those stores which support the generation of ids during the
 * insertion of records (similar to identity columns in RDMBS such as MySQL's auto-increment column type).
 *
 * @author Gunnar Morling
 */
public interface IdentityColumnAwareGridDialect extends GridDialect {

	/**
	 * Creates an empty tuple of the specified entity type. The created tuple should be "transient", i.e. no round-trip
	 * to the datastore should be performed.
	 *
	 * @param entityKeyMetadata Represents the entity type for which the tuple should be created
	 * @param tupleContext Provides additional meta-data useful for tuple creation
	 * @return
	 */
	Tuple createTuple(EntityKeyMetadata entityKeyMetadata, TupleContext tupleContext);

	/**
	 * Inserts the given tuple into the datastore, generating an id while doing so. The generated id is to be added to
	 * the given tuple (the id column name(s) can be obtained from the given entity key meta-data).
	 *
	 * @param entityKeyMetadata The type of the entity to save
	 * @param tuple The entity column values to save
	 * @param tupleContext Provides additional meta-data useful for tuple insertion
	 */
	void insertTuple(EntityKeyMetadata entityKeyMetadata, Tuple tuple, TupleContext tupleContext);
}
