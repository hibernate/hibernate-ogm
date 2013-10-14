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

import org.hibernate.ogm.options.navigation.context.EntityContext;
import org.hibernate.ogm.options.navigation.context.GlobalContext;
import org.hibernate.ogm.options.navigation.context.PropertyContext;
import org.hibernate.ogm.options.navigation.impl.BaseEntityContext;
import org.hibernate.ogm.options.navigation.impl.BaseGlobalContext;
import org.hibernate.ogm.options.navigation.impl.BasePropertyContext;
import org.hibernate.ogm.options.navigation.impl.ConfigurationContext;
import org.hibernate.ogm.test.options.examples.EmbedExampleOption;
import org.hibernate.ogm.test.options.examples.ForceExampleOption;
import org.hibernate.ogm.test.options.examples.NameExampleOption;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class SampleMappingModel {

	public interface SampleGlobalContext extends GlobalContext<SampleGlobalContext, SampleEntityContext> {
		SampleGlobalContext force(boolean force);
	}

	public interface SampleEntityContext extends EntityContext<SampleEntityContext, SamplePropertyContext> {
		// inherited
		SampleEntityContext force(boolean force);

		SampleEntityContext name(String name);
	}

	public interface SamplePropertyContext extends PropertyContext<SampleEntityContext, SamplePropertyContext> {
		SamplePropertyContext embed(Object object);
	}

	public abstract static class SampleGlobalContextImpl extends BaseGlobalContext<SampleGlobalContext> implements SampleGlobalContext {

		public SampleGlobalContextImpl(ConfigurationContext context) {
			super( context );
		}

		@Override
		public SampleGlobalContext force(boolean force) {
			addGlobalOption( ForceExampleOption.valueOf( force ) );
			return this;
		}
	}

	public abstract static class SampleEntityContextImpl extends BaseEntityContext<SampleEntityContext> implements SampleEntityContext {

		public SampleEntityContextImpl(ConfigurationContext context) {
			super( context );
		}

		@Override
		public SampleEntityContext force(boolean force) {
			addEntityOption( ForceExampleOption.valueOf( force ) );
			return this;
		}

		@Override
		public SampleEntityContext name(String name) {
			addEntityOption( new NameExampleOption( name ) );
			return this;
		}
	}

	public abstract static class SamplePropertyContextImpl extends BasePropertyContext<SamplePropertyContext> implements SamplePropertyContext {

		public SamplePropertyContextImpl(ConfigurationContext context) {
			super( context );
		}

		@Override
		public SamplePropertyContext embed(Object object) {
			addPropertyOption( new EmbedExampleOption( object ) );
			return this;
		}
	}
}
