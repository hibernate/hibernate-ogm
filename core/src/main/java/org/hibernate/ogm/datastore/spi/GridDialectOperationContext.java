/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.spi;

import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.options.spi.OptionsContext;

/**
 * Defines operations common to all context objects passed to {@link GridDialect} operations.
 *
 * @author Gunnar Morling
 */
public interface GridDialectOperationContext {

	/**
	 * Returns a context object providing access to the options effectively applying for a given entity or property.
	 */
	OptionsContext getOptionsContext();
}
