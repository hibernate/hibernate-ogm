/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.dialect.couchdb.designdocument;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.util.List;

/**
 * Represents the Result of the REST call associated with the {@link AssociationsDesignDocument} and
 * {@link EntitiesDesignDocument}
 *
 * @author Andrea Boriero <dreborier@gmail.com/>
 */
public class Rows {

	private List<Row> rows;

	List<Row> getRows() {
		return rows;
	}

	void setRows(List<Row> rows) {
		this.rows = rows;
	}

	@JsonIgnore
	public int size() {
		if ( rows.size() == 0 ) {
			return 0;
		}
		else {
			return rows.get( 0 ).getValue();
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private static class Row {
		private int value;

		public int getValue() {
			return value;
		}

		public void setValue(int value) {
			this.value = value;
		}
	}
}
