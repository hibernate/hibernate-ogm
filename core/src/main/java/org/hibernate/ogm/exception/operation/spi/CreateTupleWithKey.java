/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.exception.operation.spi;

import org.hibernate.ogm.model.key.spi.EntityKey;

/**
 * Represents one execution of
 * {@link GridDialect#createTuple(org.hibernate.ogm.model.key.spi.EntityKey, org.hibernate.ogm.dialect.spi.TupleContext).
 *
 * @author Gunnar Morling
 */
public interface CreateTupleWithKey extends GridDialectOperation {

	EntityKey getEntityKey();
}
