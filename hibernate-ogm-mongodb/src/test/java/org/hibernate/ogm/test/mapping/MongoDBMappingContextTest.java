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

import org.hibernate.ogm.mapping.impl.MappingContext;
import org.hibernate.ogm.mapping.mongodb.MongoDBMappingModel.MongoDBEntityContext;
import org.hibernate.ogm.mapping.mongodb.MongoDBMappingModel.MongoDBPropertyContext;
import org.hibernate.ogm.mapping.mongodb.MongoDBMappingServiceFactory;
import org.junit.Test;

/**
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class MongoDBMappingContextTest {
	@Test
	public void testEntityContext() throws Exception {
		MongoDBMappingServiceFactory factory = new MongoDBMappingServiceFactory();
		MappingContext context = factory.createMappingContext();
		assertThat( context.getEntityContextClass() ).isEqualTo( MongoDBEntityContext.class );
	}

	@Test
	public void testPropertyContext() throws Exception {
		MongoDBMappingServiceFactory factory = new MongoDBMappingServiceFactory();
		MappingContext context = factory.createMappingContext();
		assertThat( context.getPropertyContextClass() ).isEqualTo( MongoDBPropertyContext.class );
	}
}
