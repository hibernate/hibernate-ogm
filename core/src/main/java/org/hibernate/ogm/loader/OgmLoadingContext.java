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
package org.hibernate.ogm.loader;

import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.jdbc.TupleAsMapResultSet;

import java.util.List;

/**
 * Object holding contextual information around data loading
 * and that are OGM specific. This object is used by {@link OgmLoader}.
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class OgmLoadingContext {
	/**
	 * Do not edit this reference. Shared by everyone and still mutable.
	 */
	public static final OgmLoadingContext EMPTY_CONTEXT = new OgmLoadingContext();

	private TupleAsMapResultSet resultSet;

	public boolean hasResultSet() {
		return resultSet != null;
	}

	public TupleAsMapResultSet getResultSet() {
		return resultSet;
	}

	public void setTuples(List<Tuple> tuples) {
		if ( tuples == null ) {
			this.resultSet = null;
		}
		else {
			TupleAsMapResultSet tupleResultSet = new TupleAsMapResultSet();
			tupleResultSet.setTuples( tuples );
			this.resultSet = tupleResultSet;
		}
	}
}
