package org.hibernate.ogm.datastore.ignite.test;

import java.io.Serializable;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class ObjectId implements Serializable, Comparable<ObjectId> 
{
	private static final long serialVersionUID = 612335064581936822L;

	@Basic
	@Column(name="ID_MAJOR")
	private int majorId;
	
	@Basic
	@Column(name="ID_MINOR")
	private int minorId;
	
	@Basic
	@Column(name="ID_MEGA")
	private int megaId;
	
	public int getMegaId() {
		return megaId;
	}

	public void setMegaId(int megaId) {
		this.megaId = megaId;
	}

	public ObjectId()
	{
		this(0, 0, 0);
	}

	public ObjectId(int megaId, int majorId, int minorId)
	{
		this.megaId = megaId;
		this.majorId = majorId;
		this.minorId = minorId;
	}
	
	public int getMajorId()
	{
		return majorId;
	}
	public void setMajorId(int majorId)
	{
		this.majorId = majorId;
	}
	public int getMinorId()
	{
		return minorId;
	}
	public void setMinorId(int minorId)
	{
		this.minorId = minorId;
	}
	
	public boolean isEmpty()
	{
		return megaId == 0 && majorId == 0 && minorId == 0;
	}

	@Override
	public String toString()
	{
		return megaId + "-" + majorId + "-" + minorId;
	}

	@Override
	public int compareTo(ObjectId o)
	{
		int result = 0;
		if (o == null) 
			result = 1;
		else if (o != this)
		{ 
			ObjectId otherId = o;
			result = (megaId < otherId.getMegaId() ? -1 : (majorId == otherId.getMegaId() ? 0 : 1));
			if (result == 0) {
				result = (majorId < otherId.getMajorId() ? -1 : (majorId == otherId.getMajorId() ? 0 : 1));
			}
			if (result == 0) {
				result = (minorId < otherId.getMinorId() ? -1 : (minorId == otherId.getMinorId() ? 0 : 1));
			}
		}
		return result;
	}

	@Override
	public int hashCode()
	{
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + megaId;
		result = PRIME * result + majorId;
		result = PRIME * result + minorId;
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		final ObjectId other = (ObjectId) obj;
		if (megaId != other.megaId || majorId != other.majorId || minorId != other.minorId) return false;
		return true;
	}
}
