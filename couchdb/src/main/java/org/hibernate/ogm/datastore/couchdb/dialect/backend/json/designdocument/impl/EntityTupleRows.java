/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.couchdb.dialect.backend.json.designdocument.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.ogm.datastore.couchdb.dialect.backend.json.impl.EntityDocument;
import org.hibernate.ogm.datastore.couchdb.dialect.model.impl.CouchDBTupleSnapshot;
import org.hibernate.ogm.model.spi.Tuple;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents the Result of the REST call associated with the {@link TuplesDesignDocument}
 *
 * @author Andrea Boriero &lt;dreborier@gmail.com&gt;
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EntityTupleRows {

	private List<Row> rows;

	List<Row> getRows() {
		return rows;
	}

	void setRows(List<Row> rows) {
		this.rows = rows;
	}

	@JsonIgnore
	public List<Tuple> getTuples() {
		List<Tuple> tuples = new ArrayList<Tuple>( rows.size() );
		if ( rows.size() > 0 ) {
			for ( Row row : rows ) {
				tuples.add( new Tuple( new CouchDBTupleSnapshot( row.getValue().getProperties() ) ) );
			}
		}
		return tuples;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private static class Row {
		private EntityDocument value;

		public EntityDocument getValue() {
			return value;
		}

		@SuppressWarnings("unused") //invoked reflectively
		void setValue(EntityDocument value) {
			this.value = value;
		}
	}
}
