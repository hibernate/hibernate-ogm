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

import java.lang.annotation.ElementType;

import org.hibernate.ogm.options.generic.NamedQueryOption;
import org.hibernate.ogm.options.navigation.impl.ConfigurationContext;
import org.hibernate.ogm.options.navigation.impl.WritableOptionsServiceContext;
import org.hibernate.ogm.options.spi.OptionsContainer;
import org.hibernate.ogm.test.options.examples.EmbedExampleOption;
import org.hibernate.ogm.test.options.examples.ForceExampleOption;
import org.hibernate.ogm.test.options.examples.NameExampleOption;
import org.hibernate.ogm.test.options.mapping.model.SampleOptionModel;
import org.hibernate.ogm.test.options.mapping.model.SampleOptionModel.SampleGlobalContext;
import org.junit.Test;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class ProxyFactoryTest {

	@Test
	public void testProxyGeneration() throws Exception {
		final Object embedded = 3;

		WritableOptionsServiceContext context = new WritableOptionsServiceContext();
		SampleGlobalContext sampleMapping = SampleOptionModel.createGlobalContext( new ConfigurationContext( context ) );
		sampleMapping
				.namedQuery( "foo", "from Foo" )
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

		OptionsContainer globalOptions = context.getGlobalOptions();
		assertThat( globalOptions.getUnique( ForceExampleOption.class ) ).isTrue();
		assertThat( globalOptions.get( NamedQueryOption.class, "foo" ) ).isEqualTo( "from Foo" );

		OptionsContainer exampleEntityOptions = context.getEntityOptions( Example.class );
		assertThat( exampleEntityOptions.getUnique( ForceExampleOption.class ) ).isTrue();
		assertThat( exampleEntityOptions.getUnique( NameExampleOption.class ) ).isEqualTo( "Batman" );

		OptionsContainer sampleEntityOptions = context.getEntityOptions( Sample.class );
		assertThat( sampleEntityOptions.getUnique( ForceExampleOption.class ) ).isFalse();
		assertThat( sampleEntityOptions.getUnique( NameExampleOption.class ) ).isEqualTo( "Joker" );


		OptionsContainer titlePropertyOptions = context.getPropertyOptions( Example.class, "title" );
		assertThat( titlePropertyOptions.getUnique( EmbedExampleOption.class ) ).isEqualTo( embedded );
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
