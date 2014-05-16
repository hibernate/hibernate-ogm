/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.type;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * @author Nicolas Helleringer
 */
@Entity
public class Bookmark {

	private String id;
	private Long userId;
	private Calendar destructionCalendar;
	private BookmarkType type;
	private byte[] blob;
	private UUID serialNumber;
	private Integer stockCount;
	private Date creationDate;
	private URL url;
	private String description;
	private BigDecimal siteWeight;
	private BigInteger visitCount;
	private Byte displayMask;
	private Date updateTime;
	private Date destructionDate;
	private Calendar creationCalendar;
	private Boolean favourite;

	@Id
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public URL getUrl() {
		return url;
	}

	public void setUrl(URL url) {
		this.url = url;
	}

	@Column(name = "site_weight")
	public BigDecimal getSiteWeight() {
		return siteWeight;
	}

	public void setSiteWeight(BigDecimal siteWeight) {
		this.siteWeight = siteWeight;
	}

	@Column(name = "visits_count")
	public BigInteger getVisitCount() {
		return visitCount;
	}

	public void setVisitCount(BigInteger visitCount) {
		this.visitCount = visitCount;
	}

	@Column(name = "is_favourite")
	public Boolean isFavourite() {
		return favourite;
	}

	public void setFavourite(Boolean favourite) {
		this.favourite = favourite;
	}

	@Column(name = "display_mask")
	public Byte getDisplayMask() {
		return displayMask;
	}

	public void setDisplayMask(Byte displayMask) {
		this.displayMask = displayMask;
	}


	@Temporal(TemporalType.DATE)
	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date date) {
		this.creationDate = date;
	}

	@Temporal(TemporalType.TIME)
	public Date getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getDestructionDate() {
		return destructionDate;
	}

	public void setDestructionDate(Date destructionDate) {
		this.destructionDate = destructionDate;
	}

	@Temporal(TemporalType.DATE)
	public Calendar getCreationCalendar() {
		return creationCalendar;
	}

	public void setCreationCalendar(Calendar creationCalendar) {
		this.creationCalendar = creationCalendar;
	}

	// not supported by core today: nobody misses it apparently ;)
	// @Temporal(TemporalType.TIME)
	// public Calendar getUpdateCalendar() { return updateCalendar; }
	// public void setUpdateCalendar(Calendar updateCalendar) { this.updateCalendar = updateCalendar; }
	// private Calendar updateCalendar;

	@Temporal(TemporalType.TIMESTAMP)
	public Calendar getDestructionCalendar() {
		return destructionCalendar;
	}

	public void setDestructionCalendar(Calendar destructionCalendar) {
		this.destructionCalendar = destructionCalendar;
	}

	@Enumerated(EnumType.STRING)
	public BookmarkType getType() {
		return type;
	}

	public void setType(BookmarkType type) {
		this.type = type;
	}

	@Lob
	@Basic(fetch = FetchType.EAGER)
	@Column(name = "DS_BLOB")
	public byte[] getBlob() {
		return blob;
	}

	public void setBlob(byte[] blob) {
		this.blob = blob;
	}

	public UUID getSerialNumber() {
		return serialNumber;
	}

	public void setSerialNumber(UUID serialNumber) {
		this.serialNumber = serialNumber;
	}

	public Integer getStockCount() {
		return stockCount;
	}

	public void setStockCount(Integer stockCount) {
		this.stockCount = stockCount;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

}
