package org.hibernate.ogm.type;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.Mapping;
import org.hibernate.ogm.type.descriptor.StringMappedGridTypeDescriptor;
import org.hibernate.type.descriptor.java.BooleanTypeDescriptor;

/**
 * @author Nicolas Helleringer
 */
public class BooleanType extends AbstractGenericBasicType<Boolean> {

	public static final BooleanType INSTANCE = new BooleanType();

	public BooleanType() {
		super( StringMappedGridTypeDescriptor.INSTANCE, BooleanTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "boolean";
	}
	
	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}
	
	@Override
	public String toString(Boolean value) throws HibernateException{
		return value.toString();
	}
	
	@Override
	public Boolean fromStringValue(String value) throws HibernateException {
		if( value != "true" && value != "false") {
			throw new HibernateException("Unable to rebuild Boolean from String");
		}
		return new Boolean(value);
	}
}
