/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect;

import java.io.Closeable;
import java.util.Iterator;

import org.hibernate.ogm.datastore.spi.Tuple;

/**
 * @author Davide D'Alto <davide@hibernate.org>
 */
public interface TupleIterator extends Iterator<Tuple>, Closeable {

	@Override
	void close();

}
