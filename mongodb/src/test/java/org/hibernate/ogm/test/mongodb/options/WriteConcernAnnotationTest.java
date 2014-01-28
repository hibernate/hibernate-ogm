/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013-2014 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.test.mongodb.options;

import static org.fest.assertions.Assertions.assertThat;

import org.hibernate.ogm.datastore.mongodb.WriteConcernType;
import org.hibernate.ogm.options.mongodb.WriteConcern;
import org.hibernate.ogm.options.mongodb.impl.WriteConcernOption;
import org.hibernate.ogm.options.navigation.impl.WritableOptionsServiceContext;
import org.hibernate.ogm.options.spi.OptionsContainer;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class WriteConcernAnnotationTest {

	private WritableOptionsServiceContext optionsContext;

	@Before
	public void setupBuilder() {
		optionsContext = new WritableOptionsServiceContext();
	}

	@Test
	public void testWriteConcernForEntity() throws Exception {
		OptionsContainer options = optionsContext.getEntityOptions( EntityWriteConcernExample.class );
		assertThat( options.getUnique( WriteConcernOption.class ) ).isEqualTo( WriteConcernType.ERRORS_IGNORED );
	}

	@Test
	public void testWriteConcernForField() throws Exception {
		OptionsContainer options = optionsContext.getPropertyOptions( FieldWriteConcernExample.class, "content" );
		assertThat( options.getUnique( WriteConcernOption.class ) ).isEqualTo( WriteConcernType.FSYNCED );
	}

	@Test
	public void testWriteConcernForMethod() throws Exception {
		OptionsContainer options = optionsContext.getPropertyOptions( MethodWriteConcernExample.class, "content" );
		assertThat( options.getUnique( WriteConcernOption.class ) ).isEqualTo( WriteConcernType.JOURNALED );
	}

	@Test
	public void testWriteConcernAnnotationPriorities() throws Exception {
		OptionsContainer options = optionsContext.getEntityOptions( AnnotatedClass.class );
		assertThat( options.getUnique( WriteConcernOption.class ) ).isEqualTo( WriteConcernType.ACKNOWLEDGED );

		options = optionsContext.getPropertyOptions( AnnotatedClass.class, "title");
		assertThat( options.getUnique( WriteConcernOption.class ) ).isEqualTo( WriteConcernType.ERRORS_IGNORED );
	}

	@WriteConcern(WriteConcernType.ERRORS_IGNORED)
	private static final class EntityWriteConcernExample {
	}

	private static final class FieldWriteConcernExample {
		@WriteConcern(WriteConcernType.FSYNCED)
		public String content;
	}

	private static final class MethodWriteConcernExample {
		public String content;

		@WriteConcern(WriteConcernType.JOURNALED)
		public String getContent() {
			return content;
		}
	}

	@WriteConcern(WriteConcernType.ACKNOWLEDGED)
	private static final class AnnotatedClass {
		@WriteConcern(WriteConcernType.ERRORS_IGNORED)
		public String title;
	}
}
