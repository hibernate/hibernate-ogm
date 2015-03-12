/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.eventstate.impl;

import org.hibernate.engine.spi.SessionImplementor;

/**
 * Callback for producing event cycle scoped state objects.
 * <p>
 * Invoked by {@link EventContextManager} in case a event state type is accessed for the first time during a given event
 * cycle.
 *
 * @author Gunnar Morling
 */
public interface EventStateProducer<T> {

	T produce(SessionImplementor session);
}
