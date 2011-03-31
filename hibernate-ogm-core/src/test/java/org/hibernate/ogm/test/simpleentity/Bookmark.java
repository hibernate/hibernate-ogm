package org.hibernate.ogm.test.simpleentity;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.annotations.Persister;
import org.hibernate.ogm.persister.OgmEntityPersister;

/**
 * @author Nicolas Helleringer
 */
@Entity @Persister( impl = OgmEntityPersister.class)
public class Bookmark {

	@Id
	public String getId() { return id; }
	public void setId(String id) { this.id = id; }
	private String id;

	public String getDescription() { return description; }
	public void setDescription(String description) { this.description = description; }
	private String description;

	public URL getUrl() { return url; }
	public void setUrl(URL url ) { this.url = url; }
	private URL url; 
	
	@Column(name = "site_weigth")
	public BigDecimal getSiteWeigth() { return siteWeigth; }
	public void setSiteWeigth(BigDecimal siteWeigth) { this.siteWeigth= siteWeigth; }
	private BigDecimal siteWeigth;
	
	@Column(name = "visits_count")
	public BigInteger getVisitCount() { return visitCount; }
	public void setVisitCount(BigInteger visitCount) { this.visitCount= visitCount; }
	private BigInteger visitCount;	
	
	@Column(name = "is_favourite")
	public Boolean isFavourite() { return favourite; }
	public void setFavourite(Boolean favourite) { this.favourite= favourite; }
	private Boolean favourite;
	
	@Column(name = "display_mask")
	public Byte getDisplayMask() { return displayMask; }
	public void setDisplayMask(Byte displayMask) { this.displayMask= displayMask; }
	private Byte displayMask;
	
}
