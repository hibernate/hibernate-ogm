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
package org.hibernate.ogm.datastore.infinispan.dialect.impl;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.Set;

import org.hibernate.ogm.datastore.infinispan.InfinispanDialect;
import org.hibernate.ogm.datastore.infinispan.impl.InfinispanDatastoreProvider;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.AssociationKeyMetadata;
import org.infinispan.commons.marshall.AdvancedExternalizer;

/**
 * An externalizer for serializing and de-serializing {@link AssociationKey} instances. Implicitly used by
 * {@link InfinispanDialect} which stores keys as is in the Infinispan data store.
 * <p>
 * This externalizer is automatically registered with the cache manager when starting the
 * {@link InfinispanDatastoreProvider}, so it's not required to configure the externalizer in the Infinispan
 * configuration file.
 *
 * @author Gunnar Morling
 */
// As an implementation of AdvancedExternalizer this is never serialized according to the Externalizer docs
@SuppressWarnings("serial")
public class AssociationKeyExternalizer implements AdvancedExternalizer<AssociationKey> {

	public static final AssociationKeyExternalizer INSTANCE = new AssociationKeyExternalizer();

	/**
	 * Format version of the key type; allows to apply version dependent deserialization logic in the future if
	 * required; to be incremented when adding new fields to the serialized structure
	 */
	private static final int VERSION = 1;

	private static final Set<Class<? extends AssociationKey>> TYPE_CLASSES = Collections.<Class<? extends AssociationKey>>singleton( AssociationKey.class );

	private AssociationKeyExternalizer() {
	}

	@Override
	public void writeObject(ObjectOutput output, AssociationKey key) throws IOException {
		output.writeInt( VERSION );
		output.writeUTF( key.getTable() );
		output.writeObject( key.getColumnNames() );
		output.writeObject( key.getColumnValues() );
	}

	@Override
	public AssociationKey readObject(ObjectInput input) throws IOException, ClassNotFoundException {
		// version
		input.readInt();

		String tableName = input.readUTF();
		String[] columnNames = (String[]) input.readObject();
		Object[] values = (Object[]) input.readObject();

		AssociationKeyMetadata associationKeyMetadata = new AssociationKeyMetadata( tableName, columnNames, null, null, null, false );

		// the engine never accesses deserialized key instances so it's ok to leave the additional attributes
		// null; we should still consider extract these attributes to avoid potential confusion
		return new AssociationKey( associationKeyMetadata, values, null );
	}

	@Override
	public Set<Class<? extends AssociationKey>> getTypeClasses() {
		return TYPE_CLASSES;
	}

	@Override
	public Integer getId() {
		return ExternalizerIds.ASSOCIATION_KEY;
	}
}
