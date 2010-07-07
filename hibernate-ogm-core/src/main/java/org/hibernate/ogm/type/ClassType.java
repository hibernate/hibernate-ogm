package org.hibernate.ogm.type;

import org.hibernate.MappingException;
import org.hibernate.engine.Mapping;
import org.hibernate.ogm.type.descriptor.PassThroughGridTypeDescriptor;
import org.hibernate.type.descriptor.java.ClassTypeDescriptor;

/**
 * @author Emmanuel Bernard
 */
public class ClassType extends AbstractGenericBasicType<Class> {
	public static final ClassType INSTANCE = new ClassType();

	public ClassType() {
		super( PassThroughGridTypeDescriptor.INSTANCE, ClassTypeDescriptor.INSTANCE );
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}

	@Override
	public String getName() {
		return "class";
	}
}
