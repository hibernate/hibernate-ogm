/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.multiget.spi;

import java.util.List;

import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.spi.Tuple;

/**
 * A {@link GridDialect} facet representing dialects that can load several objects in one datastore operation.
 *
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public interface MultigetGridDialect extends GridDialect {

	/**
	 * Return the list of tuples with the given columns for a given list of keys.
	 * The tuples must be returned in the same order as the keys.
	 * If a key has no matching record, set null to the list entry.
	 * <p>
	 * All the keys provided will have the same {@link EntityKeyMetadata}.
	 * In other words they target the same "table".
	 *
	 * @param keys The array of tuple identifier
	 * @param tupleContext Contains additional information that might be used to create the tuples
	 * @return the list of tuples identified by the keys
	 */
	List<Tuple> getTuples(EntityKey[] keys, TupleContext tupleContext);
}
