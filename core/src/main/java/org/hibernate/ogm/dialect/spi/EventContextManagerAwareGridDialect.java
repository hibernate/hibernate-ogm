/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.spi;

import org.hibernate.ogm.dialect.eventstate.impl.EventContextManager;

/**
 * A {@link GridDialect} facet to be implemented by the dialects which require the injection of the EventContextManager.
 *
 * @author Guillaume Smet
 */
public interface EventContextManagerAwareGridDialect extends GridDialect {

	/**
	 * Sets the {@link EventContextManager}. Must be called after the {@link EventContextManager} has been initialized.
	 *
	 * @param eventContextManager the {@link EventContextManager}
	 */
	void setEventContextManager(EventContextManager eventContextManager);

}
