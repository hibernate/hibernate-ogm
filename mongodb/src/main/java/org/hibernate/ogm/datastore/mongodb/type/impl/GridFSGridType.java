/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.type.impl;

import org.bson.types.ObjectId;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.datastore.mongodb.type.GridFS;
import org.hibernate.ogm.type.descriptor.impl.PassThroughGridTypeDescriptor;
import org.hibernate.ogm.type.impl.AbstractGenericBasicType;

/**
 * Persists {@link ObjectId}s as is in MongoDB.
 *
 * @author Gunnar Morling
 */
public class GridFSGridType extends AbstractGenericBasicType<GridFS> {

	public static final GridFSGridType INSTANCE = new GridFSGridType();

	public GridFSGridType() {
		super( PassThroughGridTypeDescriptor.INSTANCE, GridFSJavaTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "gridfs";
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}
}
