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

import org.hibernate.ogm.options.navigation.impl.NoSqlGenerator.NoSqlEntityGenerator;
import org.hibernate.ogm.options.navigation.impl.NoSqlGenerator.NoSqlGlobalGenerator;
import org.hibernate.ogm.options.navigation.impl.NoSqlGenerator.NoSqlPropertyGenerator;
import org.hibernate.ogm.options.spi.EntityOptions;
import org.hibernate.ogm.options.spi.GlobalOptions;
import org.hibernate.ogm.options.spi.NoSqlMapping.NoSqlEntityContext;
import org.hibernate.ogm.options.spi.NoSqlMapping.NoSqlGlobalContext;
import org.hibernate.ogm.options.spi.NoSqlMapping.NoSqlPropertyContext;
import org.hibernate.ogm.options.spi.PropertyOptions;
import org.hibernate.ogm.test.options.examples.EmbedExampleOption;
import org.hibernate.ogm.test.options.examples.ForceExampleOption;
import org.hibernate.ogm.test.options.examples.NameExampleOption;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class SampleMappingModel {

	public interface SampleGlobalOptions<M> extends GlobalOptions<M> {
		M force(boolean force);
	}

	public interface SampleEntityOptions<M> extends EntityOptions<M> {
		// inherited
		M force(boolean force);

		M name(String name);
	}

	public interface SamplePropertyOptions<M> extends PropertyOptions<M> {
		M embed(Object object);
	}

	public interface SampleGlobalContext extends
			NoSqlGlobalContext<SampleGlobalContext, SampleEntityContext, SamplePropertyContext>,
			SampleGlobalOptions<SampleGlobalContext> {
	}

	public interface SampleEntityContext extends
			NoSqlEntityContext<SampleGlobalContext, SampleEntityContext, SamplePropertyContext>,
			SampleEntityOptions<SampleEntityContext> {
	}

	public interface SamplePropertyContext extends
			NoSqlPropertyContext<SampleGlobalContext, SampleEntityContext, SamplePropertyContext>,
			SamplePropertyOptions<SamplePropertyContext> {
	}

	public static class SampleGlobalOptionGenerator extends NoSqlGlobalGenerator implements SampleGlobalOptions<Object> {
		public int calledForce = 0;
		public int calledNamedQuery = 0;

		@Override
		public ForceExampleOption force(boolean force) {
			calledForce++;
			return ForceExampleOption.valueOf( force );
		}

	}

	public static class SampleEntityOptionGenerator extends NoSqlEntityGenerator implements SampleEntityOptions<Object> {
		public int calledForce = 0;
		public int calledName = 0;

		@Override
		public ForceExampleOption force(boolean force) {
			calledForce++;
			return ForceExampleOption.valueOf( force );
		}

		@Override
		public NameExampleOption name(String name) {
			calledName++;
			return new NameExampleOption( name );
		}
	}

	public static class SamplePropertyOptionGenerator extends NoSqlPropertyGenerator implements SamplePropertyOptions<Object> {
		public int calledEmbed = 0;

		@Override
		public EmbedExampleOption embed(Object object) {
			calledEmbed++;
			return new EmbedExampleOption( object );
		}
	}

}
