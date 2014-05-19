/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.massindex.batchindexing;

import org.hibernate.ogm.datastore.spi.Tuple;

/**
 * Represents an object that can consume a {@link Tuple}.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 */
public interface Consumer {

	void consume(Tuple tuple);

}
