package org.hibernate.ogm.type;

import org.hibernate.MappingException;
import org.hibernate.engine.Mapping;
import org.hibernate.ogm.type.descriptor.PassThroughGridTypeDescriptor;
import org.hibernate.type.descriptor.java.LongTypeDescriptor;

/**
 * @author Emmanuel Bernard
 */
public class LongType extends AbstractGenericBasicType<Long> {
	public static final LongType INSTANCE = new LongType();

	@SuppressWarnings({ "UnnecessaryBoxing" })
	private static final Long ZERO = Long.valueOf( 0 );

	public LongType() {
		super( PassThroughGridTypeDescriptor.INSTANCE, LongTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return "long";
	}

	@Override
	public String[] getRegistrationKeys() {
		return new String[] { getName(), long.class.getName(), Long.class.getName() };
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}
}
