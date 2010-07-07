package org.hibernate.ogm.type;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.type.AbstractStandardBasicType;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.ClassTypeDescriptor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.LongTypeDescriptor;
import org.hibernate.type.descriptor.java.StringTypeDescriptor;

/**
 * @author Emmanuel Bernard
 */
public class TypeTranslator {
	private final Map<JavaTypeDescriptor, GridType> typeConverter;

	public TypeTranslator() {
		typeConverter = new HashMap<JavaTypeDescriptor, GridType>();
		typeConverter.put( ClassTypeDescriptor.INSTANCE, ClassType.INSTANCE );
		typeConverter.put( LongTypeDescriptor.INSTANCE, LongType.INSTANCE );
		typeConverter.put( StringTypeDescriptor.INSTANCE, StringType.INSTANCE );
	}

	public GridType getType(Type type) {
		if ( type instanceof AbstractStandardBasicType ) {
			AbstractStandardBasicType exposedType = (AbstractStandardBasicType) type;
			final GridType gridType = typeConverter.get( exposedType.getJavaTypeDescriptor() );
			if (gridType == null) {
				throw new HibernateException( "Unable to find a GridType for " + exposedType.getClass().getName() );
			}
			return gridType;
		}
		throw new HibernateException( "Unable to find a GridType for " + type.getClass().getName() );
	}
}
