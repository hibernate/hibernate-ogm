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
package org.hibernate.ogm.test.options.mapping;

import java.lang.annotation.ElementType;

import org.hibernate.ogm.options.navigation.context.EntityContext;
import org.hibernate.ogm.options.navigation.context.GlobalContext;
import org.hibernate.ogm.options.navigation.context.PropertyContext;
import org.hibernate.ogm.options.navigation.impl.NoSqlEntityContextImpl;
import org.hibernate.ogm.options.navigation.impl.NoSqlGlobalContextImpl;
import org.hibernate.ogm.options.navigation.impl.NoSqlPropertyContextImpl;
import org.hibernate.ogm.options.navigation.impl.MappingContext;
import org.hibernate.ogm.test.options.examples.EmbedExampleOption;
import org.hibernate.ogm.test.options.examples.ForceExampleOption;
import org.hibernate.ogm.test.options.examples.NameExampleOption;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class SampleMappingModel {

	public interface SampleGlobalOptions<G> {

		G force(boolean force);

	}

	public interface SampleEntityOptions<E> {

		// inherited
		E force(boolean force);

		E name(String name);

	}

	public interface SamplePropertyOptions<P> {

		P embed(Object object);

	}

	public interface SampleGlobalContext extends GlobalContext<SampleGlobalContext, SampleEntityContext, SamplePropertyContext>,
			SampleGlobalOptions<SampleGlobalContext> {
	}

	public interface SampleEntityContext extends EntityContext<SampleGlobalContext, SampleEntityContext, SamplePropertyContext>,
			SampleEntityOptions<SampleEntityContext> {
	}

	public interface SamplePropertyContext extends PropertyContext<SampleGlobalContext, SampleEntityContext, SamplePropertyContext>,
			SamplePropertyOptions<SamplePropertyContext> {
	}

	public static class SampleGlobalContextImpl
			extends NoSqlGlobalContextImpl<SampleGlobalContext, SampleEntityContext, SamplePropertyContext>
			implements SampleGlobalContext {

		public SampleGlobalContextImpl(MappingContext context) {
			super( context );
		}

		@Override
		public SampleEntityContext entity(Class<?> type) {
			return entity( type, new SampleEntityContextImpl( context(), this, type ) );
		}

		@Override
		public SampleGlobalContext force(boolean force) {
			addOption( ForceExampleOption.valueOf( force ) );
			return this;
		}

	}

	public static class SampleEntityContextImpl
			extends NoSqlEntityContextImpl<SampleGlobalContext, SampleEntityContext, SamplePropertyContext>
			implements SampleEntityContext {

		public SampleEntityContextImpl(MappingContext context, SampleGlobalContext global, Class<?> type) {
			super( context, global, type );
		}

		@Override
		public SamplePropertyContext property(String propertyName, ElementType target) {
			return new SamplePropertyContextImpl( context(), this, type(), propertyName );
		}

		@Override
		public SampleEntityContext force(boolean force) {
			addOption( ForceExampleOption.valueOf( force ) );
			return this;
		}

		@Override
		public SampleEntityContext name(String name) {
			addOption( new NameExampleOption( name ) );
			return this;
		}

	}

	public static class SamplePropertyContextImpl
			extends NoSqlPropertyContextImpl<SampleGlobalContext, SampleEntityContext, SamplePropertyContext>
			implements SamplePropertyContext {

		public SamplePropertyContextImpl(MappingContext context, SampleEntityContext entity, Class<?> type, String propertyName) {
			super( context, entity, type, propertyName );
		}

		@Override
		public SamplePropertyContext embed(Object object) {
			addOption( new EmbedExampleOption( object ) );
			return this;
		}

	}

}
