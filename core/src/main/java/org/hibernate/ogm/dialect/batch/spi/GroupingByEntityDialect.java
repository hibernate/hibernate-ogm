/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.batch.spi;

import org.hibernate.ogm.dialect.spi.GridDialect;

/**
 * A {@link GridDialect} that can group operations for a given entity.
 *
 * @author Guillaume Smet
 */
public interface GroupingByEntityDialect extends GridDialect {

	/**
	 * Execute all the changes collected for a given entity.
	 *
	 * @param groupedOperation the grouped operation
	 */
	void executeGroupedChangesToEntity(GroupedChangesToEntityOperation groupedOperation);

}
