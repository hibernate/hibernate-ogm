/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.datastore.impl;

import java.util.Map;

import org.hibernate.ogm.datastore.spi.Tuple;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
//FIXME remove when done with tuple conversion for associations
public class TupleToMapHelper {
	public static void populateMapTupleByColumnName(Tuple tuple, String[] identifierColumnNames, Map<String, Object> mapTuple) {
		for(String column : identifierColumnNames) {
			Object value = tuple.get( column );
			if (value != null) {
				mapTuple.put( column, value );
			}
		}
	}

	public static Tuple getTupleFromMapTuple(Map<String,Object> mapTuple) {
		return new Tuple( new MapBasedTupleSnapshot( mapTuple ) );
	}
}
