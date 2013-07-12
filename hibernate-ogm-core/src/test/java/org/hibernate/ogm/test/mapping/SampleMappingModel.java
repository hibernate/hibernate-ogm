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
package org.hibernate.ogm.test.mapping;

import org.hibernate.ogm.mapping.context.EntityContext;
import org.hibernate.ogm.mapping.context.GlobalContext;
import org.hibernate.ogm.mapping.context.PropertyContext;
import org.hibernate.ogm.test.mapping.option.EmbedExampleOption;
import org.hibernate.ogm.test.mapping.option.NameExampleOption;
import org.hibernate.ogm.test.mapping.option.ForceOption;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class SampleMappingModel {

	public interface SampleGlobalOptions<M> {
		M force(boolean force);
	}

	public interface SampleEntityOptions<M> {
		// inherited
		M force(boolean force);

		M name(String name);
	}

	public interface SamplePropertyOptions<M> {
		M embed(Object object);
	}

	public interface SampleMapping extends
			GlobalContext<SampleMapping, SampleEntityContext, SamplePropertyContext>,
			SampleGlobalOptions<SampleMapping> {
	}

	public interface SampleEntityContext extends
			EntityContext<SampleMapping, SampleEntityContext, SamplePropertyContext>,
			SampleEntityOptions<SampleEntityContext> {
	}

	public interface SamplePropertyContext extends
			PropertyContext<SampleMapping, SampleEntityContext, SamplePropertyContext>,
			SamplePropertyOptions<SamplePropertyContext> {
	}

	public static class SampleGlobalOptionGenerator implements SampleGlobalOptions<Object> {
		public int calledForce = 0;

		@Override
		public ForceOption force(boolean force) {
			calledForce++;
			return ForceOption.valueOf( force );
		}
	}

	public static class SampleEntityOptionGenerator implements SampleEntityOptions<Object> {
		public int calledForce = 0;
		public int calledName = 0;

		@Override
		public ForceOption force(boolean force) {
			calledForce++;
			return ForceOption.valueOf( force );
		}

		@Override
		public NameExampleOption name(String name) {
			calledName++;
			return new NameExampleOption( name );
		}
	}

	public static class SamplePropertyOptionGenerator implements SamplePropertyOptions<Object> {

		public int calledEmbed = 0;

		@Override
		public EmbedExampleOption embed(Object object) {
			calledEmbed++;
			return new EmbedExampleOption( object );
		}
	}

}
