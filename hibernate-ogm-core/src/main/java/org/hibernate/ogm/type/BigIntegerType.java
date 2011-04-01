package org.hibernate.ogm.type;

import java.math.BigInteger;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.Mapping;
import org.hibernate.ogm.type.descriptor.StringMappedGridTypeDescriptor;
import org.hibernate.type.descriptor.java.BigIntegerTypeDescriptor;

/**
 * @author Nicolas Helleringer
 */
public class BigIntegerType extends AbstractGenericBasicType<BigInteger> {

	public static final BigIntegerType INSTANCE = new BigIntegerType();

	public BigIntegerType() {
		super( StringMappedGridTypeDescriptor.INSTANCE, BigIntegerTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "big_integer";
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
	public String toString(BigInteger value) throws HibernateException{
		return value.toString();
	}
	
	@Override
	public BigInteger fromStringValue(String string) throws HibernateException {
		try {
			return new BigInteger(string);
		} catch (NumberFormatException e) {
			throw new HibernateException("Unable to rebuild BigInteger from String", e);
		}
	}
}
