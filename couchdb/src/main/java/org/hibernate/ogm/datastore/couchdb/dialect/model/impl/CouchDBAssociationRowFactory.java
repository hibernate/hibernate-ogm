/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.couchdb.dialect.model.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.ogm.datastore.document.association.spi.AssociationRowFactory;
import org.hibernate.ogm.datastore.document.association.spi.AssociationRow;
import org.hibernate.ogm.datastore.document.association.spi.AssociationRow.AssociationRowAccessor;
import org.hibernate.ogm.datastore.document.association.spi.StructureOptimizerAssociationRowFactory;

/**
 * {@link AssociationRowFactory} which creates association rows based on the map based representation used in CouchDB.
 *
 * @author Gunnar Morling
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class CouchDBAssociationRowFactory extends StructureOptimizerAssociationRowFactory<Map<String, Object>> {

	public static final CouchDBAssociationRowFactory INSTANCE = new CouchDBAssociationRowFactory();

	private CouchDBAssociationRowFactory() {
		super( Map.class );
	}

	@Override
	protected Map<String, Object> getSingleColumnRow(String columnName, Object value) {
		return Collections.singletonMap( columnName, value );
	}

	@Override
	protected AssociationRowAccessor<Map<String, Object>> getAssociationRowAccessor(String[] prefixedColumns, String prefix) {
		return CouchDBAssociationRowAccessor.INSTANCE;
	}

	private static class CouchDBAssociationRowAccessor implements AssociationRow.AssociationRowAccessor<Map<String, Object>> {

		private static final CouchDBAssociationRowAccessor INSTANCE = new CouchDBAssociationRowAccessor();

		private final String prefix;
		private final List<String> prefixedColumns;

		public CouchDBAssociationRowAccessor() {
			this( null, null );
		}

		public CouchDBAssociationRowAccessor(String[] prefixedColumns, String prefix) {
			this.prefix = prefix;
			if ( prefix != null ) {
				this.prefixedColumns = Arrays.asList( prefixedColumns );
			}
			else {
				this.prefixedColumns = new ArrayList<String>( 0 );
			}
		}

		// only call if you have a prefix
		private String unprefix(String prefixedColumn) {
			return prefixedColumn.substring( prefix.length() + 1 ); //name + "."
		}

		@Override
		public Set<String> getColumnNames(Map<String, Object> row) {
			Set<String> columnNames = row.keySet();
			for ( String prefixedColumn : prefixedColumns ) {
				String unprefixedColumn = unprefix( prefixedColumn );
				if ( columnNames.contains( unprefixedColumn ) ) {
					columnNames.remove( unprefixedColumn );
					columnNames.add( prefixedColumn );
				}
			}
			return columnNames;
		}

		@Override
		public Object get(Map<String, Object> row, String column) {
			if ( prefixedColumns.contains( column ) ) {
				column = unprefix( column );
			}
			return row.get( column );
		}
	}
}
