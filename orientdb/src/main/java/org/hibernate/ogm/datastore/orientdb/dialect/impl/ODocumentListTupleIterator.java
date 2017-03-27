/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.orientdb.dialect.impl;

import java.sql.ResultSet;
import java.util.Iterator;
import java.util.List;

import org.hibernate.ogm.datastore.orientdb.logging.impl.Log;
import org.hibernate.ogm.datastore.orientdb.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.map.impl.MapTupleSnapshot;
import org.hibernate.ogm.dialect.query.spi.ClosableIterator;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.model.spi.Tuple.SnapshotType;
import com.orientechnologies.orient.core.record.impl.ODocument;

/**
 * Closable iterator through {@link ResultSet}
 *
 * @author Sergey Chernolyas &lt;sergey.chernolyas@gmail.com&gt;
 */
public class ODocumentListTupleIterator implements ClosableIterator<Tuple> {

	private static final Log log = LoggerFactory.getLogger();

	private Iterator<ODocument> docIt;

	public ODocumentListTupleIterator(List<ODocument> documents) {
		this.docIt = documents.iterator();
	}

	@Override
	public boolean hasNext() {
		return docIt.hasNext();
	}

	@Override
	public Tuple next() {
		return new Tuple( new MapTupleSnapshot( docIt.next().toMap() ), SnapshotType.UNKNOWN );
	}

	@Override
	public void remove() {
		docIt.remove();
	}

	@Override
	public void close() {

	}

}
