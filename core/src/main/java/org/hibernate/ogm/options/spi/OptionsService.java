/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.options.spi;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.service.Service;

/**
 * Access point to OGM specific metadata information.
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public interface OptionsService extends Service {

	/**
	 * The context containing all the options
	 *
	 * @return the context containing all the options that do not depend from the session
	 */
	OptionsServiceContext context();

	/**
	 * The context containing all the session dependent options
	 *
	 * @param session the session to use to obtain the value of the options
	 * @return the context containing all the options that are session dependent
	 */
	OptionsServiceContext context(SessionImplementor session);

	/**
	 * Contain a group of options separated in different scopes
	 *
	 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
	 */
	public interface OptionsServiceContext {

		/**
		 * Returns a context with the options applying on the global level, as either configured programmatically or via
		 * configuration options.
		 *
		 * @return a context with the options applying on the global level
		 */
		OptionsContext getGlobalOptions();

		/**
		 * Returns a context with the options effectively applying for the given entity, as configured programmatically,
		 * via annotations or configuration options, falling back to the global configuration level if a specific option
		 * is not specifically set for the given entity
		 *
		 * @return a context with the options effectively applying for the given entity
		 */
		OptionsContext getEntityOptions(Class<?> entityType);

		/**
		 * Returns a context with the options effectively applying for the given entity, as configured programmatically,
		 * via annotations or configuration options, falling back to the entity and global configuration levels if a
		 * specific option is not specifically set for the given property
		 *
		 * @return a context with the options effectively applying for the given property
		 */
		OptionsContext getPropertyOptions(Class<?> entityType, String propertyName);
	}
}
