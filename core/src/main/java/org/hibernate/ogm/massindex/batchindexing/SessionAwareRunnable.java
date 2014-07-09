/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.massindex.batchindexing;

import org.hibernate.Session;
import org.hibernate.ogm.datastore.spi.Tuple;

/**
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public interface SessionAwareRunnable {

	void run(Session upperSession, Tuple tuple);

}
