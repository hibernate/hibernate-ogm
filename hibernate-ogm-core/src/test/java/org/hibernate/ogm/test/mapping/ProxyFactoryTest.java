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

import org.hibernate.ogm.mapping.impl.ConfigurationProxyFactory;
import org.hibernate.ogm.mapping.impl.MappingContext;
import org.junit.Test;

import java.lang.annotation.ElementType;
import java.util.HashSet;
import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class ProxyFactoryTest {
	@Test
	public void testProxyGeneration() throws Exception {
		Set<Object> generators = new HashSet<Object>(  );
		MappingSample.SampleGlobalOptionGenerator global = new MappingSample.SampleGlobalOptionGenerator();
		generators.add( global );
		MappingSample.SampleEntityOptionGenerator entity = new MappingSample.SampleEntityOptionGenerator();
		generators.add( entity );
		MappingSample.SamplePropertyOptionGenerator property = new MappingSample.SamplePropertyOptionGenerator();
		generators.add( property );

		MappingContext context = new MappingContext(MappingSample.SampleMapping.class, generators);
		MappingSample.SampleMapping sampleMapping = ConfigurationProxyFactory.get( MappingSample.SampleMapping.class, context );
		sampleMapping
				.force( true )
				.entity( Example.class )
					.force( true )
					.property( "title", ElementType.METHOD )
						.embed()
				.entity( Sample.class )
					.force( false );
		assertThat( global.calledForce ).isEqualTo( 1 );
		assertThat( entity.calledName ).isEqualTo( 0 );
		assertThat( entity.calledForce ).isEqualTo( 2 );
		assertThat( property.calledEmbed ).isEqualTo( 1 );
	}

	public static final class Example {
		public String getTitle() { return title; }
		public void setTitle(String title) {  this.title = title; }
		private String title;

		public String getContent() { return content; }
		public void setContent(String content) {  this.content = content; }
		private String content;
	}

	public static final class Sample {
		public Integer getId() { return id; }
		public void setId(Integer id) {  this.id = id; }
		private Integer id;
	}
}
