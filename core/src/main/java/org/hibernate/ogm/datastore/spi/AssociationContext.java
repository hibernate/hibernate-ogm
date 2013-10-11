/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.datastore.spi;

import java.util.List;

/**
 * It represents all information used to load an association
 * when the relation between 2 objects is not embedded (IN_ENTITY mode).
 * Currently, the required information are: _id, rows and columns.
 * @author Guillaume Scheibel<guillaume.scheibel@gmail.com>
 */
public class AssociationContext {

	private final List<String> selectableColumns;

	public AssociationContext(List<String> selectableColumns) {
		this.selectableColumns = selectableColumns;
	}

	public List<String> getSelectableColumns() {
		return selectableColumns;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder( "Association Context {" );
		for ( int i = 0; i < this.getSelectableColumns().size(); i++ ) {
			String columnName = this.getSelectableColumns().get( i );
			builder.append( columnName );
			if ( i != this.getSelectableColumns().size() - 1 ) {
				builder.append( ", " );
			}
		}
		builder.append( "}" );
		return builder.toString();
	}
}
