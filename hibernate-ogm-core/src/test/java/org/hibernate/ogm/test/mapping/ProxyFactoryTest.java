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
package org.hibernate.ogm.test.mapping;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.test.utils.OptionContainerHelper.options;

import java.lang.annotation.ElementType;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.ogm.mapping.impl.ConfigurationProxyFactory;
import org.hibernate.ogm.mapping.impl.MappingContext;
import org.hibernate.ogm.test.mapping.option.EmbedExampleOption;
import org.hibernate.ogm.test.mapping.option.NameExampleOption;
import org.hibernate.ogm.test.mapping.option.ForceOption;
import org.junit.Test;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class ProxyFactoryTest {

	@Test
	public void testProxyGeneration() throws Exception {
		SampleMappingModel.SampleGlobalOptionGenerator global = new SampleMappingModel.SampleGlobalOptionGenerator();
		SampleMappingModel.SampleEntityOptionGenerator entity = new SampleMappingModel.SampleEntityOptionGenerator();
		SampleMappingModel.SamplePropertyOptionGenerator property = new SampleMappingModel.SamplePropertyOptionGenerator();

		Set<Object> generators = new HashSet<Object>();
		generators.add( global );
		generators.add( entity );
		generators.add( property );

		final Object embedded = 3;
		MappingContext context = new MappingContext(SampleMappingModel.SampleMapping.class, generators);
		SampleMappingModel.SampleMapping sampleMapping = ConfigurationProxyFactory.get( SampleMappingModel.SampleMapping.class, context );
		sampleMapping
				.force( true )
				.entity( Example.class )
					.force( true )
					.property( "title", ElementType.FIELD )
						.embed( embedded )
				.entity( Sample.class )
					.name( "Joker" )
					.force( false )
				.entity( Example.class )
					.name( "Batman" );

		assertThat( global.calledForce ).isEqualTo( 1 );
		assertThat( entity.calledName ).isEqualTo( 2 );
		assertThat( entity.calledForce ).isEqualTo( 2 );
		assertThat( property.calledEmbed ).isEqualTo( 1 );

		assertThat( options( context ) )
			.hasSize( 1 )
			.contains( ForceOption.TRUE );
		assertThat( options( context, Example.class ) )
			.hasSize( 2 )
			.contains( ForceOption.TRUE, new NameExampleOption( "Batman" ) );
		assertThat( options( context, Sample.class ) )
			.hasSize( 2 )
			.contains( ForceOption.FALSE, new NameExampleOption( "Joker" ) );
		assertThat( options( context, Example.class, "title" ) )
			.hasSize( 1 )
			.contains( new EmbedExampleOption( embedded ) );
	}

	public static final class Example {
		private String title;
		private String content;

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getContent() {
			return content;
		}

		public void setContent(String content) {
			this.content = content;
		}
	}

	public static final class Sample {
		private Integer id;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}
}
