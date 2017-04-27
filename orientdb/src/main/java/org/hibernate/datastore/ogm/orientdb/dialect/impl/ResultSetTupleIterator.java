/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.datastore.ogm.orientdb.dialect.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.hibernate.datastore.ogm.orientdb.constant.OrientDBConstant;
import org.hibernate.datastore.ogm.orientdb.logging.impl.Log;
import org.hibernate.datastore.ogm.orientdb.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.map.impl.MapTupleSnapshot;
import org.hibernate.ogm.dialect.query.spi.ClosableIterator;
import org.hibernate.ogm.model.spi.Tuple;

/**
 * @author Sergey Chernolyas (sergey.chernolyas@gmail.com)
 */
public class ResultSetTupleIterator implements ClosableIterator<Tuple> {

	private static final Log log = LoggerFactory.getLogger();

	private final ResultSet resultSet;

	public ResultSetTupleIterator(ResultSet resultSet) {
		this.resultSet = resultSet;
	}

	@Override
	public boolean hasNext() {
		try {
			return !resultSet.isLast();
		}
		catch (SQLException e) {
			log.error( "Error with ResultSet", e );
			throw new RuntimeException( e );
		}
	}

	@Override
	public Tuple next() {
		try {
			resultSet.next();
			return convert();
		}
		catch (SQLException e) {
			log.error( "Error with ResultSet", e );
			throw new RuntimeException( e );
		}
	}

	protected Tuple convert() throws SQLException {
		Map<String, Object> map = new HashMap<>();
		for ( int i = 0; i < resultSet.getMetaData().getColumnCount(); i++ ) {
			int fieldNum = i + 1;
			map.put( resultSet.getMetaData().getColumnName( fieldNum ), resultSet.getObject( fieldNum ) );
		}
		for ( String systemField : OrientDBConstant.SYSTEM_FIELDS ) {
			map.put( systemField, resultSet.getObject( systemField ) );
		}

		log.info( "field map: " + map );
		return new Tuple( new MapTupleSnapshot( map ) );
	}

	@Override
	public void remove() {
		try {
			resultSet.deleteRow();
		}
		catch (SQLException e) {
			log.error( "Error with ResultSet", e );
		}
	}

	@Override
	public void close() {
		try {
			resultSet.close();
		}
		catch (SQLException e) {
			log.error( "Error with ResultSet", e );
		}
	}

}
