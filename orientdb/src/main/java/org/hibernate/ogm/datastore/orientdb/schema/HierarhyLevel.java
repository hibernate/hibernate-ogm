/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.orientdb.schema;

import org.hibernate.mapping.Table;

public class HierarhyLevel {

	private Table table;
	private Integer level;

	public Table getTable() {
		return table;
	}

	public void setTable(Table table) {
		this.table = table;
	}

	public Integer getLevel() {
		return level;
	}

	public void setLevel(Integer level) {
		this.level = level;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( table == null ) ? 0 : table.hashCode() );
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		HierarhyLevel other = (HierarhyLevel) obj;
		if ( table == null ) {
			if ( other.table != null ) {
				return false;
			}
		}
		else if ( !table.equals( other.table ) ) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "HierarhyLevel{" + "table=" + table.getName() + ", level=" + level + '}';
	}
}
