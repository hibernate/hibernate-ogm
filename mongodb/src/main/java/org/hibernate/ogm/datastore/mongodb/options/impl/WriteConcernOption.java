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
package org.hibernate.ogm.datastore.mongodb.options.impl;

import org.hibernate.ogm.datastore.mongodb.MongoDBProperties;
import org.hibernate.ogm.datastore.mongodb.options.WriteConcernType;
import org.hibernate.ogm.options.spi.UniqueOption;
import org.hibernate.ogm.util.configurationreader.impl.ConfigurationPropertyReader;

import com.mongodb.WriteConcern;

/**
 * Option for specifying the {@link WriteConcern} in MongoDB.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 * @see http://docs.mongodb.org/manual/reference/write-concern/
 */
public class WriteConcernOption extends UniqueOption<WriteConcern> {

	/**
	 * The default write concern.
	 *
	 * @see MongoDBProperties#WRITE_CONCERN
	 */
	private static final WriteConcernType DEFAULT_WRITE_CONCERN = WriteConcernType.ACKNOWLEDGED;

	@Override
	public WriteConcern getDefaultValue(ConfigurationPropertyReader propertyReader) {
		WriteConcernType writeConcernType = propertyReader.property( MongoDBProperties.WRITE_CONCERN, WriteConcernType.class )
			.withDefault( DEFAULT_WRITE_CONCERN )
			.getValue();

		// load/instantiate custom type
		if ( writeConcernType == WriteConcernType.CUSTOM ) {
			return propertyReader.property( MongoDBProperties.WRITE_CONCERN_TYPE, WriteConcern.class )
				.instantiate()
				.required()
				.getValue();
		}
		// take pre-defined value
		else {
			return writeConcernType.getWriteConcern();
		}
	}
}
