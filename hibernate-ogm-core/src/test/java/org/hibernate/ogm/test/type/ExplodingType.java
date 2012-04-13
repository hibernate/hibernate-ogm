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
package org.hibernate.ogm.test.type;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.type.AbstractGenericBasicType;
import org.hibernate.ogm.type.descriptor.GridTypeDescriptor;
import org.hibernate.ogm.type.descriptor.GridValueBinder;
import org.hibernate.ogm.type.descriptor.GridValueExtractor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.UUIDTypeDescriptor;

import java.util.UUID;

/**
* @author Emmanuel Bernard <emmanuel@hibernate.org>
*/
public class ExplodingType extends AbstractGenericBasicType<UUID> {

	public static final ExplodingType INSTANCE = new ExplodingType();

	public ExplodingType() {
		super( ExplodingTypeDescriptor.INSTANCE, UUIDTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "uuid";
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}

	@Override
	public int getColumnSpan(Mapping mapping) {
		return 1;
	}

	@Override
	public String toString(UUID value) throws HibernateException {
		throw new RuntimeException( "Exploding type" );
	}

	@Override
	public UUID fromStringValue(String string) throws HibernateException {
		throw new RuntimeException( "Exploding type" );
	}

	static class ExplodingTypeDescriptor implements GridTypeDescriptor {
		public static ExplodingTypeDescriptor INSTANCE = new ExplodingTypeDescriptor();

		@Override
		public <X> GridValueBinder<X> getBinder(JavaTypeDescriptor<X> javaTypeDescriptor) {
			return new GridValueBinder<X>() {
				@Override
				public void bind(Tuple resultset, X value, String[] names) {
					throw new RuntimeException( "Exploding type" );
				}
			};
		}

		@Override
		public <X> GridValueExtractor<X> getExtractor(JavaTypeDescriptor<X> javaTypeDescriptor) {
			return new GridValueExtractor<X>() {
				@Override
				public X extract(Tuple resultset, String name) {
					throw new RuntimeException( "Exploding type" );
				}
			};
		}
	}
}
