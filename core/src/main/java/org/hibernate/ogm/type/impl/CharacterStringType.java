/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.type.impl;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.type.descriptor.impl.StringMappedGridTypeDescriptor;
import org.hibernate.type.descriptor.java.CharacterTypeDescriptor;

/**
 * Persist a {@link Character} as a {@link String}.
 *
 * Note:
 * For example necessary in MongoDB, since the MongoDB driver 3 no longer does that for us
 * https://jira.mongodb.org/browse/JAVA-1804
 *
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public class CharacterStringType extends AbstractGenericBasicType<Character>  {

	public static final CharacterStringType INSTANCE = new CharacterStringType();

	public CharacterStringType() {
		super( StringMappedGridTypeDescriptor.INSTANCE, CharacterTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "character_string";
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}
}
