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

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.test.utils.OptionContainerHelper.options;

import java.lang.annotation.ElementType;

import org.hibernate.ogm.options.navigation.impl.MappingContext;
import org.hibernate.ogm.test.options.examples.EmbedExampleOption;
import org.hibernate.ogm.test.options.examples.ForceExampleOption;
import org.hibernate.ogm.test.options.examples.NameExampleOption;
import org.hibernate.ogm.test.options.mapping.SampleMappingModel.SampleGlobalContext;
import org.junit.Test;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class ProxyFactoryTest {

	@Test
	public void testProxyGeneration() throws Exception {
		final Object embedded = 3;

		MappingContext context = new MappingContext();
		SampleGlobalContext sampleMapping = new SampleMappingFactory().createMapping( context );
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

		assertThat( options( context ) )
			.hasSize( 1 )
			.contains( ForceExampleOption.TRUE );
		assertThat( options( context, Example.class ) )
			.hasSize( 2 )
			.contains( ForceExampleOption.TRUE, new NameExampleOption( "Batman" ) );
		assertThat( options( context, Sample.class ) )
			.hasSize( 2 )
			.contains( ForceExampleOption.FALSE, new NameExampleOption( "Joker" ) );
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
