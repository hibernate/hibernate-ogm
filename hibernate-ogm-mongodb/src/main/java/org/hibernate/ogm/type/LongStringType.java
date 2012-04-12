package org.hibernate.ogm.type;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.type.descriptor.StringMappedGridTypeDescriptor;
import org.hibernate.type.descriptor.java.LongTypeDescriptor;

/**
 * 
 * @author Oliver Carr ocarr@redhat.com
 *
 */
public class LongStringType extends AbstractGenericBasicType<Long>  {

	public static final LongStringType INSTANCE = new LongStringType();

	public LongStringType() {
		super( StringMappedGridTypeDescriptor.INSTANCE, LongTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "sec";
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}

}

