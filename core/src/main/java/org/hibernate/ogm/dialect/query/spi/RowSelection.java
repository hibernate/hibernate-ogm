/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.query.spi;

/**
 * Represents the selection criteria of a query. Modelled after {@code RowSelection} in Hibernate ORM, providing only
 * those values needed for OGM's purposes at this point.
 *
 * @author Gunnar Morling
 */
public class RowSelection {

	private final Integer firstRow;
	private final Integer maxRows;

	public RowSelection(Integer firstRow, Integer maxRows) {
		this.firstRow = firstRow;
		this.maxRows = maxRows;
	}

	public static RowSelection fromOrmRowSelection(org.hibernate.engine.spi.RowSelection rowSelection) {
		return new RowSelection( rowSelection.getFirstRow(), rowSelection.getMaxRows() );
	}

	public Integer getFirstRow() {
		return firstRow;
	}

	public Integer getMaxRows() {
		return maxRows;
	}
}
