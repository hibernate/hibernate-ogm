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
package org.hibernate.ogm.datastore.mongodb.test.options.writeconcern;

import static org.fest.assertions.Assertions.assertThat;

import org.hibernate.ogm.datastore.mongodb.options.WriteConcern;
import org.hibernate.ogm.datastore.mongodb.options.WriteConcernType;
import org.hibernate.ogm.datastore.mongodb.options.impl.WriteConcernOption;
import org.hibernate.ogm.options.container.impl.OptionsContainer;
import org.hibernate.ogm.options.navigation.source.impl.AnnotationOptionValueSource;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class WriteConcernAnnotationTest {

	private AnnotationOptionValueSource source;

	@Before
	public void setupBuilder() {
		source = new AnnotationOptionValueSource();
	}

	@Test
	public void testWriteConcernForEntity() throws Exception {
		OptionsContainer options = source.getEntityOptions( EntityWriteConcernExample.class );
		assertThat( options.getUnique( WriteConcernOption.class ) ).isEqualTo( com.mongodb.WriteConcern.ERRORS_IGNORED );
	}

	@Test
	public void testWriteConcernByTypeForEntity() throws Exception {
		OptionsContainer options = source.getEntityOptions( EntityWriteConcernUsingTypeExample.class );
		assertThat( options.getUnique( WriteConcernOption.class ) ).isEqualTo( new MultipleDataCenters() );
	}

	@WriteConcern(WriteConcernType.ERRORS_IGNORED)
	private static final class EntityWriteConcernExample {
	}

	@WriteConcern(value = WriteConcernType.CUSTOM, type = MultipleDataCenters.class)
	private static final class EntityWriteConcernUsingTypeExample {
	}

	@SuppressWarnings("serial")
	public static class MultipleDataCenters extends com.mongodb.WriteConcern {

		public MultipleDataCenters() {
			super( "MultipleDataCenters", 0, false, true, false );
		}
	}
}
