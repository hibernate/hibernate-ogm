/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012-2014 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.datastore.mongodb;

import org.hibernate.ogm.cfg.DocumentStoreProperties;
import org.hibernate.ogm.cfg.OgmConfiguration;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.mongodb.options.AssociationDocumentType;
import org.hibernate.ogm.datastore.mongodb.options.ReadPreferenceType;
import org.hibernate.ogm.datastore.mongodb.options.WriteConcernType;

import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;

/**
 * Properties for configuring the MongoDB datastore via {@code persistence.xml} or {@link OgmConfiguration}.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 * @author Gunnar Morling
 */
public final class MongoDBProperties implements DocumentStoreProperties {

	/**
	 * Configuration property for defining the acknowledgement of write operations. Supported values are the
	 * {@link WriteConcernType} enum or the String representations of its constants.
	 * <p>
	 * Specify {@link WriteConcernType#CUSTOM} in conjunction with {@link #WRITE_CONCERN_TYPE} to specify a custom
	 * {@link WriteConcern} implementation.
	 * <p>
	 * Defaults to {@link WriteConcernType#ACKNOWLEDGED}.
	 *
	 * @see WriteConcern
	 */
	public static final String WRITE_CONCERN = "hibernate.ogm.mongodb.write_concern";

	/**
	 * Configuration property for setting a custom {@link WriteConcern} implementation. Can be given as fully-qualified
	 * class name, class-object or instance of the implementation type. If not given as instance, the specified type
	 * must have a default (no-args) constructor.
	 * <p>
	 * Only takes affect if {@link #WRITE_CONCERN} is set to {@link WriteConcernType#CUSTOM}.
	 */
	public static final String WRITE_CONCERN_TYPE = "hibernate.ogm.mongodb.write_concern_type";

	/**
	 * Configuration property for setting the read preference. Supported values are the {@link ReadPreferenceType} enum
	 * or the String representations of its constants.
	 * <p>
	 * Defaults to {@link ReadPreferenceType#PRIMARY}.
	 *
	 * @see ReadPreference
	 */
	public static final String READ_PREFERENCE = "hibernate.ogm.mongodb.read_preference";

	/**
	 * The timeout used at the connection to the MongoDB instance. This value is set in milliseconds. Defaults to 5000.
	 */
	public static final String TIMEOUT = "hibernate.ogm.mongodb.connection_timeout";

	/**
	 * Configuration property for specifying how to store association documents. Only applicable if
	 * {@link DocumentStoreProperties#ASSOCIATIONS_STORE} is set to {@link AssociationStorageType#ASSOCIATION_DOCUMENT}.
	 * Supported values are the {@link AssociationDocumentType} enum or the String representations of its constants.
	 * Defaults to {@link AssociationDocumentType#GLOBAL_COLLECTION}.
	 */
	public static final String ASSOCIATION_DOCUMENT_STORAGE = "hibernate.ogm.mongodb.association_document_storage";

	private MongoDBProperties() {
	}
}
