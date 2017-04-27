/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.datastore.ogm.orientdb.type.spi;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.type.descriptor.impl.PassThroughGridTypeDescriptor;
import org.hibernate.ogm.type.impl.AbstractGenericBasicType;
import com.orientechnologies.orient.core.id.ORecordId;
import org.hibernate.datastore.ogm.orientdb.type.descriptor.java.ORecordIdTypeDescriptor;

/**
 * @author Sergey Chernolyas (sergey.chernolyas@gmail.com)
 */
public class ORecordIdGridType extends AbstractGenericBasicType<ORecordId> {

	public static final ORecordIdGridType INSTANCE = new ORecordIdGridType();

	public ORecordIdGridType() {
		super( PassThroughGridTypeDescriptor.INSTANCE, ORecordIdTypeDescriptor.INSTANCE );
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}

	@Override
	public String getName() {
		return "ORecordId";
	}

}
