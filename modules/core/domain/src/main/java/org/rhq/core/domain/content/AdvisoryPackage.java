/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.core.domain.content;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.Table;

/**
 * This is the many-to-many entity that correlates an advisory with a package. 
 *
 * @author Pradeep Kilambi
 */

@Entity
@Table(name = "RHQ_ADVISORY_PACKAGE")
public class AdvisoryPackage implements Serializable {

    private static final long serialVersionUID = 1L;

    /*
     * http://opensource.atlassian.com/projects/hibernate/browse/EJB-286 Hibernate seems to want these mappings in the
     * @IdClass and ignore these here, even though the mappings should be here and no mappings should be needed in the
     * @IdClass.
     */
    @Id
    @ManyToOne
    @JoinColumn(name = "ADVISORY_ID", referencedColumnName = "ID", nullable = false, insertable = false, updatable = false)
    private Advisory advisory;

    @Id
    @ManyToOne
    @JoinColumn(name = "PACKAGE_ID", referencedColumnName = "ID", nullable = false, insertable = false, updatable = false)
    private Package pkg;

    @Column(name = "CTIME", nullable = false)
    private long createdTime;

    @Column(name = "LAST_MODIFIED", nullable = false)
    private long lastModifiedDate;

    protected AdvisoryPackage() {
    }

    public AdvisoryPackage(Advisory adv, Package pkg) {
        this.advisory = adv;
        this.pkg = pkg;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public Advisory getAdvisory() {
        return advisory;
    }

    public void setAdvisory(Advisory advisory) {
        this.advisory = advisory;
    }

    public Package getPkg() {
        return pkg;
    }

    public void setPkg(Package pkg) {
        this.pkg = pkg;
    }

    public long getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(long lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    @PrePersist
    void onPersist() {
        this.createdTime = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("AdvisoryPackage: ");
        str.append("ctime=[").append(new Date(this.createdTime)).append("]");
        str.append(", Advisory=[").append(this.advisory).append("]");
        str.append(", Package=[").append(this.pkg).append("]");
        return str.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = (31 * result) + ((advisory == null) ? 0 : advisory.hashCode());
        result = (31 * result) + ((pkg == null) ? 0 : pkg.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (!(obj instanceof AdvisoryPackage))) {
            return false;
        }

        final AdvisoryPackage other = (AdvisoryPackage) obj;

        if (advisory == null) {
            if (advisory != null) {
                return false;
            }
        } else if (!advisory.equals(other.advisory)) {
            return false;
        }

        if (pkg == null) {
            if (pkg != null) {
                return false;
            }
        } else if (!pkg.equals(other.pkg)) {
            return false;
        }

        return true;
    }
}