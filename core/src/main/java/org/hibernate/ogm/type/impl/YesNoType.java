/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.type.impl;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.type.descriptor.impl.CharMappedGridTypeDescriptor;

/**
 * Maps {@link Boolean} to {@code Y} or {@code N} characters.
 *
 * @author Gunnar Morling
 */
public class YesNoType extends AbstractGenericBasicType<Boolean> {

	public static final YesNoType INSTANCE = new YesNoType();

	public YesNoType() {
		super( CharMappedGridTypeDescriptor.INSTANCE, org.hibernate.type.YesNoType.INSTANCE.getJavaTypeDescriptor() );
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}

	@Override
	public String getName() {
		return "yes_no";
	}
}
