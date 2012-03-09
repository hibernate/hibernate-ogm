/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 *  JBoss, Home of Professional Open Source
 *  Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 *  as indicated by the @authors tag. All rights reserved.
 *  See the copyright.txt in the distribution for a
 *  full listing of individual contributors.
 *
 *  This copyrighted material is made available to anyone wishing to use,
 *  modify, copy, or redistribute it subject to the terms and conditions
 *  of the GNU Lesser General Public License, v. 2.1.
 *  This program is distributed in the hope that it will be useful, but WITHOUT A
 *  WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 *  You should have received a copy of the GNU Lesser General Public License,
 *  v.2.1 along with this distribution; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 *  MA  02110-1301, USA.
 */

package org.hibernate.ogm.type.cassandra;

import java.util.UUID;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.type.AbstractGenericBasicType;
import org.hibernate.ogm.type.descriptor.PassThroughGridTypeDescriptor;
import org.hibernate.type.descriptor.java.LongTypeDescriptor;
import org.hibernate.type.descriptor.java.UUIDTypeDescriptor;

/**
 * For {@link java.util.Calendar} objects use a String representation for MongoDB.
 *
 * @author Oliver Carr ocarr@redhat.com
 *
 */
public class UUIDType extends AbstractGenericBasicType<UUID> {

	public static final UUIDType INSTANCE = new UUIDType();

	public UUIDType() {
		super( PassThroughGridTypeDescriptor.INSTANCE, UUIDTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return "uuid";
	}

	@Override
	public String[] getRegistrationKeys() {
		return new String[] { getName(), long.class.getName(), Long.class.getName() };
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}

}
