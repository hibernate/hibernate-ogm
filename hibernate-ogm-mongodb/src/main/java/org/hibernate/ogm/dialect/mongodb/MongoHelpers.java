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
package org.hibernate.ogm.dialect.mongodb;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.RowKey;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * @author Alan Fitton <alan at eth0.org.uk>
 */
public class MongoHelpers {
	
	public static DBObject associationKeyToObject(AssociationKey key) {
		DBObject obj = new BasicDBObject();
		Map<String,Object> fieldMap = new HashMap<String,Object>();

		int i = 0;
		Object[] columnValues = key.getColumnValues();
		for (String name : key.getColumnNames())
			fieldMap.put(name, columnValues[i++]);

		obj.put(MongoDBDialect.ASSOCIATIONS_FIELDNAME, fieldMap);
		obj.put(MongoDBDialect.TABLE_FIELDNAME, key.getTable());
		
		return obj;
	}
	
}
