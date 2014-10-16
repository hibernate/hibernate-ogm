/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.query.spi;

import java.io.Closeable;
import java.util.Iterator;

/**
 * An iterator that has to be closed.
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public interface ClosableIterator<T> extends Iterator<T>, Closeable {

	@Override
	void close();

}
