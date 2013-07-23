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
package org.hibernate.ogm.dialect.couchdb.type;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.type.AbstractGenericBasicType;
import org.hibernate.ogm.type.descriptor.StringMappedGridTypeDescriptor;
import org.hibernate.type.descriptor.java.LongTypeDescriptor;

/**
 * @author Andrea Boriero <dreborier@gmail.com/>
 */
public class CouchDBLongType extends AbstractGenericBasicType<Long> {

	public static final CouchDBLongType INSTANCE = new CouchDBLongType();

	public CouchDBLongType() {
		super( StringMappedGridTypeDescriptor.INSTANCE, LongTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "couchdb_long";
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}

}
