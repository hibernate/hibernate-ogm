/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.ogm.type.impl;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.type.descriptor.impl.WrappedGridTypeDescriptor;
import org.hibernate.type.descriptor.java.CharacterTypeDescriptor;

/**
 * Represents a Character type
 *
 * @author Ajay Bhat
 */
public class CharacterType extends AbstractGenericBasicType<Character> {
	public static CharacterType INSTANCE = new CharacterType();

	public CharacterType() {
		super( WrappedGridTypeDescriptor.INSTANCE, CharacterTypeDescriptor.INSTANCE );
	}

	@Override
	public String[] getRegistrationKeys() {
		return new String[] {getName(), char.class.getName(), Character.class.getName()};
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}

	@Override
	public String getName() {
		return "char";
	}
}
