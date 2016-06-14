/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.batch.spi;

import org.hibernate.ogm.model.key.spi.EntityKey;

/**
 * Represents an {@link Operation} we can group by entity
 *
 * @author Guillaume Smet
 */
public interface GroupableEntityOperation extends Operation {

	EntityKey getEntityKey();

}
