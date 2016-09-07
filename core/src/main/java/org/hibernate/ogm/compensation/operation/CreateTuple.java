/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.compensation.operation;

import org.hibernate.ogm.dialect.identity.spi.IdentityColumnAwareGridDialect;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;

/**
 * Represents one execution of
 * {@link IdentityColumnAwareGridDialect#createTuple(EntityKeyMetadata, org.hibernate.ogm.dialect.spi.OperationContext)}.
 *
 * @author Gunnar Morling
 */
public interface CreateTuple extends GridDialectOperation {

	EntityKeyMetadata getEntityKeyMetadata();
}
