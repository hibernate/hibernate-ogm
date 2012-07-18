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

import org.hibernate.ogm.mapping.impl.MappingContext;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class MappingContextTest {
	@Test
	public void testContext() throws Exception {
		Set<Object> generators = new HashSet<Object>(  );
		generators.add( new MappingSample.SampleGlobalOptionGenerator() );
		generators.add( new MappingSample.SampleEntityOptionGenerator() );
		generators.add( new MappingSample.SamplePropertyOptionGenerator() );

		MappingContext context = new MappingContext(MappingSample.SampleMapping.class, generators);
		assertThat( context.getEntityContextClass() ).isEqualTo( MappingSample.SampleEntityContext.class );
		assertThat( context.getPropertyContextClass() ).isEqualTo( MappingSample.SamplePropertyContext.class );
	}
}
