/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.flushstate.impl;

import org.hibernate.engine.spi.SessionImplementor;

/**
 * Callback for initializing flush cycle state objects. Invoked by {@link FlushCycleStateManager} in case
 * a flush cycle state type is accessed for the first time during a given flush cycle.
 * @author Gunnar Morling
 *
 */
public interface FlushCyleStateInitializer<T> {

	T initialize(SessionImplementor session);
}
