package org.hibernate.ogm.type;

import java.math.BigDecimal;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.Mapping;
import org.hibernate.ogm.type.descriptor.StringMappedGridTypeDescriptor;
import org.hibernate.type.descriptor.java.BigDecimalTypeDescriptor;

/**
 * @author Nicolas Helleringer
 */
public class BigDecimalType extends AbstractGenericBasicType<BigDecimal> {

	public static final BigDecimalType INSTANCE = new BigDecimalType();

	public BigDecimalType() {
		super( StringMappedGridTypeDescriptor.INSTANCE, BigDecimalTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "big_decimal";
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
	public String toString(BigDecimal value) throws HibernateException{
		return value.toString();
	}
	
	@Override
	public BigDecimal fromStringValue(String string) throws HibernateException {
		try {
			return new BigDecimal(string);
		} catch (NumberFormatException e) {
			throw new HibernateException("Unable to rebuild BigDecimal from String", e);
		}
	}
}
