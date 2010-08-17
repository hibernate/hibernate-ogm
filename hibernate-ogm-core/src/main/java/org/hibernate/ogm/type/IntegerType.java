package org.hibernate.ogm.type;

import org.hibernate.MappingException;
import org.hibernate.engine.Mapping;
import org.hibernate.ogm.type.descriptor.PassThroughGridTypeDescriptor;
import org.hibernate.type.descriptor.java.IntegerTypeDescriptor;
import org.hibernate.type.descriptor.java.LongTypeDescriptor;

/**
 * Represents a Long type
 * 
 * @author Emmanuel Bernard
 */
public class IntegerType extends AbstractGenericBasicType<Integer> {
	public static final IntegerType INSTANCE = new IntegerType();

	public IntegerType() {
		super( PassThroughGridTypeDescriptor.INSTANCE, IntegerTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return "integer";
	}

	@Override
	public String[] getRegistrationKeys() {
		return new String[] { getName(), int.class.getName(), Integer.class.getName() };
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}
}
