/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.couchdb.dialect.model.impl;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.hibernate.ogm.datastore.document.association.spi.AssociationRowFactory;
import org.hibernate.ogm.datastore.document.association.spi.AssociationRow;
import org.hibernate.ogm.datastore.document.association.spi.AssociationRow.AssociationRowAccessor;
import org.hibernate.ogm.datastore.document.association.spi.SingleColumnAwareAssociationRowFactory;

/**
 * {@link AssociationRowFactory} which creates association rows based on the map based representation used in CouchDB.
 *
 * @author Gunnar Morling
 */
public class CouchDBAssociationRowFactory extends SingleColumnAwareAssociationRowFactory<Map<String, Object>> {

	public static final CouchDBAssociationRowFactory INSTANCE = new CouchDBAssociationRowFactory();

	private CouchDBAssociationRowFactory() {
		super( Map.class );
	}

	@Override
	protected Map<String, Object> getSingleColumnRow(String columnName, Object value) {
		return Collections.singletonMap( columnName, value );
	}

	@Override
	protected AssociationRowAccessor<Map<String, Object>> getAssociationRowAccessor() {
		return CouchDBAssociationRowAccessor.INSTANCE;
	}

	private static class CouchDBAssociationRowAccessor implements AssociationRow.AssociationRowAccessor<Map<String, Object>> {

		private static final CouchDBAssociationRowAccessor INSTANCE = new CouchDBAssociationRowAccessor();

		@Override
		public Set<String> getColumnNames(Map<String, Object> row) {
			return row.keySet();
		}

		@Override
		public Object get(Map<String, Object> row, String column) {
			return row.get( column );
		}
	}
}
