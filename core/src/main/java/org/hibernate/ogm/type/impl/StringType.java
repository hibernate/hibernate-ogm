/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.type.impl;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.type.descriptor.impl.PassThroughGridTypeDescriptor;
import org.hibernate.type.descriptor.java.StringTypeDescriptor;

/**
 * @author Emmanuel Bernard
 */
public class StringType extends AbstractGenericBasicType<String> {

	public static final StringType INSTANCE = new StringType();

	public StringType() {
		super( PassThroughGridTypeDescriptor.INSTANCE, StringTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "string";
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}

	public String stringToObject(String xml) throws Exception {
		return xml;
	}

	@Override
	public String toString(String value) {
		return value;
	}
	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}
}
