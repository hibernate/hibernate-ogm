/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.eventstate.impl;

import java.util.Collections;
import java.util.Map;

/**
 * Holds all known {@link EventStateProducer}s.
 *
 * @author Gunnar Morling
 */
public class EventStateProducers {

	private EventStateProducers() {
	}

	public static Map<Class<?>, EventStateProducer<?>> getProducers(Map<?, ?> configuration) {
		return Collections.emptyMap();
	}
}
