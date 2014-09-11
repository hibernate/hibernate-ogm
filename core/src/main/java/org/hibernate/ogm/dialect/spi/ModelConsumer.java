/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.spi;

import org.hibernate.ogm.model.spi.Tuple;

/**
 * Represents an object that can consume a model element (only {@link Tuple}s atm.).
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public interface ModelConsumer {

	/**
	 * Processes the given tuple.
	 *
	 * @param tuple the tuple to process
	 */
	void consume(Tuple tuple);

}
