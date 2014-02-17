/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.dialect.infinispan.impl;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;

import org.hibernate.ogm.datastore.infinispan.impl.InfinispanDatastoreProvider;
import org.hibernate.ogm.dialect.infinispan.InfinispanDialect;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.AssociationKeyMetadata;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.EntityKeyMetadata;
import org.hibernate.ogm.grid.Key;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.util.impl.CollectionHelper;
import org.infinispan.commons.marshall.AdvancedExternalizer;

/**
 * An externalizer for serializing and de-serializing {@link Key} instances. Implicitly used by
 * {@link InfinispanDialect} which stores keys as is in the Infinispan data store.
 * <p>
 * The serialized form comprises a byte indicating the specific {@code Key} sub-type (see {@link KeyType}), followed by
 * table name, column names and column values.
 * <p>
 * This externalizer is automatically registered with the cache manager when starting the
 * {@link InfinispanDatastoreProvider}, so it's not required to configure the externalizer in the Infinispan
 * configuration file.
 *
 * @author Gunnar Morling
 */
// As an implementation of AdvancedExternalizer this is never serialized according to the Externalizer docs
@SuppressWarnings("serial")
public class KeyExternalizer implements AdvancedExternalizer<Key> {

	/**
	 * The id of this externalizer.
	 */
	private static final int ID = 4287;
	/**
	 * The specific types supported by this externalizer
	 */
	@SuppressWarnings("unchecked")
	private static final Set<Class<? extends Key>> TYPE_CLASSES = CollectionHelper.<Class<? extends Key>>asSet( EntityKey.class, AssociationKey.class,
			RowKey.class );

	@Override
	public void writeObject(ObjectOutput output, Key key) throws IOException {
		output.writeByte( KeyType.fromJavaClass( key.getClass() ).getIdentifier() );
		output.writeUTF( key.getTable() );
		output.writeObject( key.getColumnNames() );
		output.writeObject( key.getColumnValues() );
	}

	@Override
	public Key readObject(ObjectInput input) throws IOException, ClassNotFoundException {
		KeyType keyType = KeyType.fromIdentifier( input.readByte() );
		String tableName = input.readUTF();
		String[] columnNames = (String[]) input.readObject();
		Object[] values = (Object[]) input.readObject();

		switch ( keyType ) {
			case ENTITY_KEY:
				EntityKeyMetadata entityKeyMetadata = new EntityKeyMetadata( tableName, columnNames );
				return new EntityKey( entityKeyMetadata, values );
			case ASSOCIATION_KEY:
				AssociationKeyMetadata associationKeyMetadata = new AssociationKeyMetadata( tableName, columnNames );

				// the engine never accesses deserialized key instances so it's ok to leave the additional attributes
				// null; we should still consider extract these attributes to avoid potential confusion
				return new AssociationKey( associationKeyMetadata, values, null, null, null );
			case ROW_KEY:
				return new RowKey( tableName, columnNames, values );
			default:
				throw new IllegalArgumentException( "Unexpected key type: " + keyType );
		}
	}

	@Override
	public Set<Class<? extends Key>> getTypeClasses() {
		return TYPE_CLASSES;
	}

	@Override
	public Integer getId() {
		return ID;
	}

	/**
	 * Represents a specific sub-type of {@link Key}.
	 *
	 * @author Gunnar Morling
	 */
	private enum KeyType {

		ENTITY_KEY((byte) 1, EntityKey.class),

		ASSOCIATION_KEY((byte) 2, AssociationKey.class),

		ROW_KEY((byte) 3, RowKey.class);

		private final byte identifier;
		private final Class<? extends Key> javaClass;

		private KeyType(byte identifier, Class<? extends Key> javaClass) {
			this.identifier = identifier;
			this.javaClass = javaClass;
		}

		public byte getIdentifier() {
			return identifier;
		}

		public Class<? extends Key> getJavaClass() {
			return javaClass;
		}

		public static KeyType fromJavaClass(Class<? extends Key> keyType) {
			if ( keyType == ENTITY_KEY.getJavaClass() ) {
				return ENTITY_KEY;
			}
			else if ( keyType == ASSOCIATION_KEY.getJavaClass() ) {
				return ASSOCIATION_KEY;
			}
			else if ( keyType == ROW_KEY.getJavaClass() ) {
				return ROW_KEY;
			}
			else {
				throw new IllegalArgumentException( "Unexpected key type: " + keyType );
			}
		}

		public static KeyType fromIdentifier(byte identifier) {
			if ( identifier == ENTITY_KEY.getIdentifier() ) {
				return ENTITY_KEY;
			}
			else if ( identifier == ASSOCIATION_KEY.getIdentifier() ) {
				return ASSOCIATION_KEY;
			}
			else if ( identifier == ROW_KEY.getIdentifier() ) {
				return ROW_KEY;
			}
			else {
				throw new IllegalArgumentException( "Unexpected identifier: " + identifier );
			}
		}
	}
}
